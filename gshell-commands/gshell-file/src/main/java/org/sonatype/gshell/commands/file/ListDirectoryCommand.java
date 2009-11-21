/*
 * Copyright (C) 2009 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sonatype.gshell.commands.file;

import com.google.inject.Inject;
import jline.console.ConsoleReader;
import org.sonatype.gshell.command.Command;
import org.sonatype.gshell.command.CommandContext;
import org.sonatype.gshell.command.IO;
import org.sonatype.gshell.console.completer.FileNameCompleter;
import org.sonatype.gshell.util.FileAssert;
import org.sonatype.gshell.util.ansi.AnsiString;
import org.sonatype.gshell.util.cli.Argument;
import org.sonatype.gshell.util.cli.Option;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.Attribute.*;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * List the contents of a file or directory.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
@Command(name="ls")
public class ListDirectoryCommand
    extends FileCommandSupport
{
    @Argument
    private String path;

    @Option(name = "-l", aliases = {"--long"})
    private boolean longList;

    @Option(name = "-a", aliases = {"--all"})
    private boolean includeHidden;

    @Option(name = "-r", aliases = {"--recursive"})
    private boolean recursive;

    @Inject
    public ListDirectoryCommand installCompleters(final FileNameCompleter c1) {
        assert c1 != null;
        setCompleters(c1, null);
        return this;
    }

    public Object execute(final CommandContext context) throws Exception {
        assert context != null;
        IO io = context.getIo();

        File file = resolveFile(context, path);

        new FileAssert(file).exists();

        if (file.isDirectory()) {
            listChildren(io, file);
        }
        else {
            io.info(file.getPath());
        }

        return Result.SUCCESS;
    }

    private void listChildren(final IO io, final File dir) throws Exception {
        assert io != null;
        assert dir != null;

        File[] files;

        if (includeHidden) {
            files = dir.listFiles();
        }
        else {
            files = dir.listFiles(new FileFilter()
            {
                public boolean accept(final File file) {
                    assert file != null;
                    return !file.isHidden();
                }
            });
        }

        ConsoleReader reader = new ConsoleReader(io.streams.in, io.out, null, io.getTerminal());
        reader.setPaginationEnabled(false);

        List<CharSequence> names = new ArrayList<CharSequence>(files.length);
        List<File> dirs = new LinkedList<File>();

        for (File file : files) {
            if (hasChildren(file)) {
                if (recursive) {
                    dirs.add(file);
                }
            }

            names.add(render(file));
        }

        if (longList) {
            for (CharSequence name : names) {
                io.out.println(name);
            }
        }
        else {
            reader.printColumns(names);
        }

        if (!dirs.isEmpty()) {
            for (File subDir : dirs) {
                io.out.println();
                io.out.print(subDir.getName());
                io.out.print(":");
                listChildren(io, subDir);
            }
        }
    }

    private CharSequence render(final File file) {
        assert file != null;

        String name = file.getName();

        if (file.isDirectory()) {
            name = ansi().fg(BLUE).a(name).a(File.separator).reset().toString();
        }
        else if (file.canExecute()) {
            name = ansi().fg(GREEN).a(name).a("*").reset().toString();
        }

        if (file.isHidden()) {
            name = ansi().a(INTENSITY_FAINT).a(name).reset().toString();
        }

        return new AnsiString(name);
    }
}