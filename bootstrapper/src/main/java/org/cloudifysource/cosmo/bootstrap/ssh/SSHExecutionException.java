/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.cloudifysource.cosmo.bootstrap.ssh;

/**
 * An exception that is thrown if an ssh execution failed.
 * The cause may be an exit status different the 0 or some {@link java.io.IOException}
 * @author Dan Kilman
 * @since 0.1
 */
public class SSHExecutionException extends RuntimeException {

    private final int exitStatus;

    public SSHExecutionException(int exitStatus) {
        super("SSH Execution returned a non-zero status. Exit status [" + exitStatus + "]");
        this.exitStatus = exitStatus;
    }

    public int getExitStatus() {
        return exitStatus;
    }

}
