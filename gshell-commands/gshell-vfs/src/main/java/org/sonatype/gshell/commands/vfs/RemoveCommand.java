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

package org.sonatype.gshell.commands.vfs;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.Selectors;
import org.sonatype.gshell.command.Command;
import org.sonatype.gshell.command.CommandContext;
import org.sonatype.gshell.command.IO;
import org.sonatype.gshell.util.cli.Argument;
import org.sonatype.gshell.vfs.FileObjects;

/**
 * Remove a file or directory.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
@Command(name="rm")
public class RemoveCommand
    extends VfsCommandSupport
{
    @Argument(required=true)
    private String path;

    // TODO: Add --recursive support

    // TODO: Add --verbose support

    public Object execute(final CommandContext context) throws Exception {
        assert context != null;
        IO io = context.getIo();

        FileObject file = resolveFile(context, path);

        ensureFileExists(file);

        try {
            file.delete(Selectors.SELECT_SELF);
        }
        finally {
            FileObjects.close(file);
        }

        return Result.SUCCESS;
    }
}