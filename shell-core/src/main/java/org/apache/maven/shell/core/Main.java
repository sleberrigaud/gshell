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

package org.apache.maven.shell.core;

import org.apache.maven.shell.Shell;
import org.apache.maven.shell.ShellHolder;
import org.apache.maven.shell.VariableNames;
import org.apache.maven.shell.Variables;
import org.apache.maven.shell.ansi.Ansi;
import org.apache.maven.shell.cli.Argument;
import org.apache.maven.shell.cli.Option;
import org.apache.maven.shell.cli.Printer;
import org.apache.maven.shell.cli.Processor;
import org.apache.maven.shell.core.impl.ShellImpl;
import org.apache.maven.shell.core.impl.registry.CommandRegistrationAgent;
import org.apache.maven.shell.i18n.MessageSource;
import org.apache.maven.shell.i18n.ResourceBundleMessageSource;
import org.apache.maven.shell.io.AnsiAwareIO;
import org.apache.maven.shell.io.IO;
import org.apache.maven.shell.io.SystemInputOutputHijacker;
import org.apache.maven.shell.notification.ExitNotification;
import org.apache.maven.shell.terminal.AutoDetectedTerminal;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Command-line bootstrap for Apache Maven Shell (<tt>mvnsh</tt>).
 *
 * @version $Rev$ $Date$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class Main
    implements VariableNames
{
    private static final String DEBUG = "DEBUG";

    private static final String TRACE = "TRACE";

    private static final String INFO = "INFO";

    private static final String ERROR = "ERROR";

    private static final String WARN = "WARN";

    private final IO io = new AnsiAwareIO();

    private final Variables vars = new Variables();

    private final MessageSource messages = new ResourceBundleMessageSource(getClass());

    //
    // TODO: Add flag to capture output to log file
    //

    @Option(name="-h", aliases={"--help"}, requireOverride=true)
    private boolean help;

    @Option(name="-V", aliases={"--version"}, requireOverride=true)
    private boolean version;

    @Option(name="-e", aliases={"--exception"})
    private void setShowExceptionTraces(final boolean flag) {
        if (flag) {
            vars.set(MVNSH_SHOW_STACKTRACE, Boolean.TRUE);
        }
    }

    private void setConsoleLogLevel(final String level) {
        //
        // TODO: Use a variable to control this, so users can change... can we do that?
        //

        System.setProperty("mvnsh.log.console.level", level);
    }

    @Option(name="-d", aliases={"--debug"})
    private void setDebug(boolean flag) {
        if (flag) {
            setConsoleLogLevel(DEBUG);
            io.setVerbosity(IO.Verbosity.DEBUG);
        }
    }

    @Option(name="-X", aliases={"--trace"})
    private void setTrace(boolean flag) {
        if (flag) {
            setConsoleLogLevel(TRACE);
            io.setVerbosity(IO.Verbosity.DEBUG);
        }
    }

    @Option(name="-v", aliases={"--verbose"})
    private void setVerbose(boolean flag) {
        if (flag) {
            setConsoleLogLevel(INFO);
            io.setVerbosity(IO.Verbosity.VERBOSE);
        }
    }

    @Option(name="-q", aliases={"--quiet"})
    private void setQuiet(boolean flag) {
        if (flag) {
            setConsoleLogLevel(ERROR);
            io.setVerbosity(IO.Verbosity.QUIET);
        }
    }

    @Option(name="-c", aliases={"--commands"})
    private String commands;

    @Argument()
    private List<String> commandArgs = null;

    //
    // TODO: Expose setting variables via the command-line
    //
    
    @Option(name="-D", aliases={"--define"})
    private void setSystemProperty(final String nameValue) {
        assert nameValue != null;

        String name, value;
        int i = nameValue.indexOf('=');

        if (i == -1) {
            name = nameValue;
            value = Boolean.TRUE.toString();
        }
        else {
            name = nameValue.substring(0, i);
            value = nameValue.substring(i + 1, nameValue.length());
        }
        name = name.trim();

        System.setProperty(name, value);
    }

    @Option(name="-C", aliases={"--color"}, argumentRequired=true)
    private void enableAnsiColors(final boolean flag) {
        Ansi.setEnabled(flag);
    }

    @Option(name="-T", aliases={"--terminal"}, argumentRequired=true)
    private void setTerminalType(final String type) {
        AutoDetectedTerminal.configure(type);
    }

    public void boot(final String[] args) throws Exception {
        assert args != null;

        // Setup environment defaults
        setShowExceptionTraces(false);
        setTerminalType(AutoDetectedTerminal.AUTO);
        setConsoleLogLevel(WARN);

        // Process command line options & arguments
        Processor clp = new Processor(this);
        clp.setStopAtNonOption(true);
        clp.process(args);

        if (help) {
            Printer printer = new Printer(clp);
            printer.setMessageSource(messages);
            printer.printUsage(io.out, System.getProperty(MVNSH_PROGRAM));
            io.flush();
            System.exit(ExitNotification.DEFAULT_CODE);
        }

        if (version) {
            // TODO: Expose MVNSH_PROGRAM_TITLE or something

            io.out.format("%s %s", System.getProperty(MVNSH_PROGRAM), System.getProperty(MVNSH_VERSION)).println();
            io.flush();
            System.exit(ExitNotification.DEFAULT_CODE);
        }

        // setUp a reference for our exit code so our callback thread can tell if we've shutdown normally or not
        final AtomicReference<Integer> codeRef = new AtomicReference<Integer>();
        int code = ExitNotification.DEFAULT_CODE;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (codeRef.get() == null) {
                    // Give the user a warning when the JVM shutdown abnormally, normal shutdown
                    // will set an exit code through the proper channels

                    io.err.println();
                    io.err.println(messages.getMessage("warning.abnormalShutdown"));
                }

                io.flush();
            }
        });

        try {
            ContainerConfiguration config = new DefaultContainerConfiguration();

            // TODO: Hookup Plexus logging to Slf4j

            PlexusContainer container = new DefaultPlexusContainer(config);

            // Hijack the system output streams
            if (!SystemInputOutputHijacker.isInstalled()) {
                SystemInputOutputHijacker.install();
            }
            
            // Register the IO streams
            SystemInputOutputHijacker.register(io.streams);

            //
            // TODO: Bring back the ShellBuilder (lives in shell-core, not a plexus component)
            //

            // Create the shell instance
            ShellImpl shell = (ShellImpl)container.lookup(Shell.class);
            shell.setIo(io);
            shell.setVariables(vars);

            // Install shell into thread context
            ShellHolder.set(shell);

            // Register our commands
            CommandRegistrationAgent agent = container.lookup(CommandRegistrationAgent.class);
            agent.registerCommands();
            
            // clp gives us a list, but we need an array
            String[] _args = {};
            if (commandArgs != null) {
                commandArgs.toArray(new String[commandArgs.size()]);
            }

            if (commands != null) {
                shell.execute(commands);
            }
            else {
                shell.run((Object[])_args);
            }
        }
        catch (ExitNotification n) {
            code = n.code;
        }
        finally {
            io.flush();
        }

        codeRef.set(code);

        System.exit(code);
    }

    public static void main(final String[] args) throws Exception {
        Main main = new Main();
        main.boot(args);
    }
}
