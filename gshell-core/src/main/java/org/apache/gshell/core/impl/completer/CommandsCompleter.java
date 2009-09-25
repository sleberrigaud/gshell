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

package org.apache.gshell.core.impl.completer;

import jline.ArgumentCompletor;
import jline.Completor;
import org.apache.gshell.command.Command;
import org.apache.gshell.console.completer.AggregateCompleter;
import org.apache.gshell.console.completer.StringsCompleter;
import org.apache.gshell.console.completer.TerminalCompleter;
import org.apache.gshell.core.impl.registry.CommandRegisteredEvent;
import org.apache.gshell.core.impl.registry.CommandRemovedEvent;
import org.apache.gshell.event.EventListener;
import org.apache.gshell.event.EventManager;
import org.apache.gshell.registry.CommandRegistry;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Completor} for commands, including support for command-specific sub-completion.
 *
 * Keeps up to date automatically by handling command-related events.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 1.0
 */
@Component(role=Completor.class, hint="commands")
public class CommandsCompleter
    implements Completor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Requirement
    private EventManager eventManager;

    @Requirement
    private CommandRegistry commandRegistry;

    private final Map<String,Completor> completors = new HashMap<String,Completor>();

    private final AggregateCompleter delegate = new AggregateCompleter();

    private boolean initialized;

    public CommandsCompleter() {}
    
    public CommandsCompleter(final EventManager eventManager, final CommandRegistry commandRegistry) {
        assert eventManager != null;
        this.eventManager = eventManager;
        assert commandRegistry != null;
        this.commandRegistry = commandRegistry;
    }

    private void init() {
        try {
            // Populate the initial list of completers from the currently registered commands
            Collection<String> names = commandRegistry.getCommandNames();
            for (String name : names) {
                addCompleter(name);
            }

            // Register for updates to command registrations
            eventManager.addListener(new EventListener() {
                public void onEvent(final EventObject event) throws Exception {
                    if (event instanceof CommandRegisteredEvent) {
                        CommandRegisteredEvent targetEvent = (CommandRegisteredEvent)event;
                        addCompleter(targetEvent.getName());
                    }
                    else if (event instanceof CommandRemovedEvent) {
                        CommandRemovedEvent targetEvent = (CommandRemovedEvent)event;
                        removeCompleter(targetEvent.getName());
                    }
                }
            });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        initialized = true;
    }

    private void addCompleter(final String name) throws Exception {
        assert name != null;

        log.trace("Adding completer for: {}", name);
        
        List<Completor> children = new ArrayList<Completor>();

        // Attach completion for the command name
        children.add(new StringsCompleter(name));

        // Then attach any command specific completers
        Command command = commandRegistry.getCommand(name);

        Completor[] completers = command.getCompleters();
        if (completers == null) {
            children.add(TerminalCompleter.INSTANCE);
        }
        else {
            for (Completor completer : completers) {
                log.trace("Adding completer: {}", completer);
                children.add(completer != null ? completer : TerminalCompleter.INSTANCE);
            }
        }

        // Setup the root completer for the command
        Completor root = new ArgumentCompletor(children);

        // Track and attach
        completors.put(name, root);
        delegate.getCompleters().add(root);
    }

    private void removeCompleter(final String name) {
        assert name != null;

        Completor completer = completors.remove(name);
        delegate.getCompleters().remove(completer);
    }

    public int complete(final String buffer, final int cursor, final List candidates) {
        if (!initialized) {
            init();
        }

        return delegate.complete(buffer, cursor, candidates);
    }
}