from cloudify.decorators import workflow
from cloudify.plugins.lifecycle import _host_pre_stop, _host_post_start, is_host_node


def _modify_agents(ctx, deployment_id, instance_operations_generator, op_name):
    for dep_id, dep_ctx in ctx.deployments_contexts.iteritems():
        if deployment_id is not None and dep_id != deployment_id:
            continue
        ctx.logger.info('%sing agents for %s' % (op_name, dep_id))
        with dep_ctx:
            graph = dep_ctx.graph_mode()
            for node_instance in dep_ctx.node_instances:
                if is_host_node(node_instance):
                    msg = '%sing agent for %s' % (op_name, node_instance.id)
                    instance_subgraph = graph.subgraph(msg)
                    sequence = instance_subgraph.sequence()
                    sequence.add(node_instance.send_event(msg))
                    sequence.add(*instance_operations_generator(node_instance))
                    sequence.add(node_instance.send_event('%sed agent for %s' % (op_name, node_instance.id)))
            graph.execute()
        ctx.logger.info('%sed agents for %s' % (op_name, dep_id)) # check if succeeded


@workflow(system_wide=True)
def uninstall(ctx, deployment_id=None):
    _modify_agents(ctx, deployment_id, _host_pre_stop, 'Uninstall')


@workflow(system_wide=True)
def install(ctx, deployment_id=None):
    _modify_agents(ctx, deployment_id, _host_post_start, 'Install')

