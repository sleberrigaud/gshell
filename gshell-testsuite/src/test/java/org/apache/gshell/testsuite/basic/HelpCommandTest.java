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

package org.apache.gshell.testsuite.basic;

import org.apache.gshell.core.commands.HelpCommand;
import org.apache.gshell.testsuite.CommandTestSupport;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests for the {@link HelpCommand}.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class HelpCommandTest
    extends CommandTestSupport
{
    public HelpCommandTest() {
        super("help", HelpCommand.class);
    }

    @Test
    public void testHelpHelp() throws Exception {
        assertTrue(commandRegistry.containsCommand("help"));
        assertFalse(aliasRegistry.containsAlias("foo"));
        Object result = executeWithArgs("help");
        assertEqualsSuccess(result);
    }

    @Test
    public void testHelpFoo() throws Exception {
        assertFalse(commandRegistry.containsCommand("foo"));
        assertFalse(aliasRegistry.containsAlias("foo"));
        Object result = executeWithArgs("foo");
        assertEqualsFailure(result);
    }
}