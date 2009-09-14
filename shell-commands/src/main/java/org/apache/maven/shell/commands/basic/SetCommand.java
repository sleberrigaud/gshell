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

package org.apache.maven.shell.commands.basic;

import org.apache.maven.shell.Shell;
import org.apache.maven.shell.Variables;
import org.apache.maven.shell.console.completer.AggregateCompleter;
import org.apache.maven.shell.cli.Argument;
import org.apache.maven.shell.cli.Option;
import org.apache.maven.shell.command.Command;
import org.apache.maven.shell.command.CommandContext;
import org.apache.maven.shell.command.CommandSupport;
import org.apache.maven.shell.command.Arguments;
import org.apache.maven.shell.i18n.MessageSource;
import org.apache.maven.shell.io.IO;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Properties;
import java.util.List;

import jline.Completor;

/**
 * Set a variable or property.
 *
 * @version $Rev$ $Date$
 */
@Component(role=Command.class, hint="set", instantiationStrategy="per-lookup")
public class SetCommand
    extends CommandSupport
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private enum Mode
    {
        VARIABLE,
        PROPERTY
    }

    @Requirement(role=Completor.class, hints={"variable-name"})
    private List<Completor> completers;

    @Option(name="-m", aliases={"--mode"})
    private Mode mode = Mode.VARIABLE;

    @Option(name="-v", aliases={"--verbose"})
    private boolean verbose;

    @Argument(index=0)
    private String name;

    @Argument(index=1, multiValued=true)
    private List<String> values = null;

    public String getName() {
        return "set";
    }

    @Override
    public Completor[] getCompleters() {
        assert completers != null;

        return new Completor[] {
            new AggregateCompleter(completers),
            null
        };
    }
    
    public Object execute(final CommandContext context) throws Exception {
        assert context != null;
        IO io = context.getIo();
        MessageSource messages = getMessages();

        if (name == null) {
            return displayList(context);
        }
        else if (values == null) {
            io.error("Missing required argument: {}", messages.getMessage("command.argument.values.token"));
            return Result.FAILURE;
        }

        String value = Arguments.asString(values, " ");

        switch (mode) {
            case PROPERTY:
                log.debug("Setting system property: {}={}", name, value);
                System.setProperty(name, value);
                break;

            case VARIABLE:
                Variables vars = context.getVariables();
                if (!name.startsWith(Shell.SHELL_INTERNAL)) {
                    log.info("Setting variable: {}={}", name, value);
                    vars.set(name, value);
                }
                break;
        }

        return Result.SUCCESS;
    }

    private Object displayList(final CommandContext context) throws Exception {
        assert context != null;
        IO io = context.getIo();

        // NOTE: Using io.outputStream to display values to avoid any ANSI encoding or other translation.
        
        switch (mode) {
            case PROPERTY: {
                Properties props = System.getProperties();

                for (Object o : props.keySet()) {
                    String name = (String) o;
                    String value = props.getProperty(name);

                    io.outputStream.print(name);
                    io.outputStream.print("='");
                    io.outputStream.print(value);
                    io.outputStream.print("'");

                    // Value is always a string, so no need to add muck here for --verbose

                    io.outputStream.println();
                }
                break;
            }

            case VARIABLE: {
                Variables variables = context.getVariables();
                Iterator<String> iter = variables.names();

                while (iter.hasNext()) {
                    String name = iter.next();

                    // HACK: Hide some internal muck for now
                    if (name.startsWith(Shell.SHELL_INTERNAL)) {
                        continue;
                    }

                    Object value = variables.get(name);

                    io.outputStream.print(name);
                    io.outputStream.print("='");
                    io.outputStream.print(value);
                    io.outputStream.flush();
                    io.outputStream.print("'");

                    // When --verbose include the class details of the values
                    if (verbose && value != null) {
                        io.outputStream.print(" (");
                        io.outputStream.print(value.getClass());
                        io.outputStream.print(")");
                    }

                    io.outputStream.println();
                }
                break;
            }
        }

        // Manually flush the stream, normally framework only flushes io.out
        io.outputStream.flush();

        return Result.SUCCESS;
    }
}