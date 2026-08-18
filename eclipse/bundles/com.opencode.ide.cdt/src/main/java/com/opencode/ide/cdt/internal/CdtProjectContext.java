package com.opencode.ide.cdt.internal;

import java.nio.file.Path;
import java.util.Optional;

import com.opencode.ide.core.context.ProjectContext;

/**
 * CDT-backed {@link ProjectContext} - the integration seam between the
 * opencode core/UI layers and the C/C++ (CDT) layer.
 *
 * <p><b>Status: STUB.</b> Registered as an OSGi Declarative Service
 * (see {@code OSGI-INF/CdtProjectContext.xml}) so core/UI can discover it
 * without importing CDT packages. It does not yet call any CDT API. A later
 * phase will resolve the active {@code ICProject}'s worktree so that a spawned
 * {@code opencode serve} runs in the right C/C++ project, and so the opencode
 * {@code /project} context tracks the active edit target.</p>
 */
public class CdtProjectContext implements ProjectContext {

    @Override
    public Optional<Path> getWorkingDirectory() {
        // TODO(cdt): resolve the active ICProject via CDT model APIs and return its worktree.
        return Optional.empty();
    }
}
