package org.cloudifysource.cosmo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.cloudifysource.cosmo.agent.state.AgentState;
import org.cloudifysource.cosmo.mock.MockAgent;
import org.cloudifysource.cosmo.mock.MockManagement;
import org.cloudifysource.cosmo.mock.MockTaskContainer;
import org.cloudifysource.cosmo.mock.MockTaskContainers;
import org.cloudifysource.cosmo.mock.TaskConsumerRegistrar;
import org.cloudifysource.cosmo.time.MockCurrentTimeProvider;
import org.junit.AfterClass;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.log.TextFormatter;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.logging.Logger;

public abstract class AbstractServiceGridTest<T extends MockManagement> {

    private MockTaskContainers containers;
    private final Logger logger;
    private T management;
    private MockCurrentTimeProvider timeProvider;
    private TaskConsumerRegistrar taskConsumerRegistrar;
    private final LifecycleStateMachine serviceLifecycleStateMachine;

    public AbstractServiceGridTest() {
        logger = Logger.getLogger(this.getClass().getName());
        setSimpleLoggerFormatter(logger);

        serviceLifecycleStateMachine = new LifecycleStateMachine
            ("machine_reachable->service_cleaned<->service_stopped<->service_started");
        serviceLifecycleStateMachine.setFinalLifecycle("service_started");
        serviceLifecycleStateMachine.setInitialLifecycle("service_cleaned");
        Assert.assertEquals(serviceLifecycleStateMachine.getNextInstanceLifecycle("service_cleaned",
                "service_started"),"service_stopped");
    }

    @BeforeClass
    public void beforeClass() {

        timeProvider = new MockCurrentTimeProvider(System.currentTimeMillis());
        taskConsumerRegistrar = new TaskConsumerRegistrar() {

            @Override
            public void registerTaskConsumer(
                    final Object taskConsumer, final URI taskConsumerId) {

                MockTaskContainer container = getManagement().newContainer(taskConsumerId, taskConsumer);
                containers.addContainer(container);
            }

            @Override
            public Object unregisterTaskConsumer(final URI taskConsumerId) {
                MockTaskContainer mockTaskContainer = containers.findContainer(taskConsumerId);
                boolean removed = containers.removeContainer(mockTaskContainer);
                Preconditions.checkState(removed, "Failed to remove container " + taskConsumerId);
                return mockTaskContainer.getTaskConsumer();
            }
        };

        management = createMockManagement();
        management.setTaskConsumerRegistrar(taskConsumerRegistrar);
        management.setTimeProvider(timeProvider);
        containers = new MockTaskContainers(management);
    }

    protected abstract T createMockManagement();

    @BeforeMethod
    public void beforeMethod(Method method) {
        containers.clear();
        timeProvider.reset(System.currentTimeMillis());
        management.start();
        logger.info("Before " + method.getName());
    }

    @AfterMethod(alwaysRun=false)
    public void afterMethod(Method method) {
        logger.info("After " + method.getName());
        try {
            if (management != null) {
                management.stop();
            }
            if (containers != null) {
                Assert.assertTrue(containers.isEmpty(),
                        "Cleanup failure in test " + method.getName() + ":" +
                                Iterables.toString(containers.getContainerIds()));
            }
        }
        finally {
            if (containers != null) {
                containers.clear();
            }
        }
    }

    @AfterClass
    public void afterClass() {
        management.close();
    }

    private static void setSimpleLoggerFormatter(final Logger logger) {
        Logger parentLogger = logger;
        while (parentLogger.getHandlers().length == 0) {
            parentLogger = logger.getParent();
        }

        parentLogger.getHandlers()[0].setFormatter(new TextFormatter());
    }

    protected T getManagement() {
        return this.management;
    }

    protected void execute(URI ... taskProducers) {
        containers.execute(taskProducers);
    }

    protected long currentTimeMillis() {
        return timeProvider.currentTimeMillis();
    }

    /**
     * This method simulates failure of the agent, and immediate restart by a reliable watchdog
     * running on the same machine
     */
    protected void restartAgent(URI agentId) {

        MockAgent agent = (MockAgent) getTaskConsumerRegistrar().unregisterTaskConsumer(agentId);
        AgentState agentState = agent.getState();
        Preconditions.checkState(agentState.isMachineReachableLifecycle());
        agentState.incrementNumberOfAgentStarts();
        getTaskConsumerRegistrar().registerTaskConsumer(new MockAgent(agentState), agentId);
    }

    public TaskConsumerRegistrar getTaskConsumerRegistrar() {
        return taskConsumerRegistrar;
    }


    /**
     * This method simulates an unexpected crash of a machine
     */
    protected void killMachine(URI agentId) {
        containers.findContainer(agentId).killMachine();
    }

    public LifecycleStateMachine getServiceLifecycleStateMachine() {
        return serviceLifecycleStateMachine;
    }
}
