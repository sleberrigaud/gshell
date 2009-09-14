/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.shell.testsuite;

import org.apache.maven.shell.Shell;
import org.apache.maven.shell.core.impl.registry.CommandRegistrationAgent;
import org.apache.maven.shell.io.IO;
import org.apache.maven.shell.io.IOHolder;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Tests that the shell can boot up.
 *
 * @version $Rev$ $Date$
 */
public abstract class ShellTestSupport
    extends PlexusTestCase
{
    private Shell shell;

    protected void setUp() throws Exception {
        super.setUp();

        IOHolder.set(new TestIO());

        CommandRegistrationAgent agent = lookup(CommandRegistrationAgent.class);
        agent.registerCommands();

        shell = lookup(Shell.class);
    }

    protected Shell getShell() {
        return shell;
    }

    protected Object execute(final String line) throws Exception {
        assertNotNull(line);
        return getShell().execute(line);
    }
}