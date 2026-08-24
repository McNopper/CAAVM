package com.opencode.ide.ui.model;

import java.nio.file.Path;

/**
 * Pure (SWT-free) spawn-cwd sanity cross-check for the Server view's project
 * header: whether the opencode server's resolved project directory and the
 * active Eclipse workspace project point at the same directory — the mismatch
 * a wrongly-launched server produces. The view supplies both sides (the
 * server's from {@link ProjectVcs#projectPath()}, the workspace's from the
 * workbench selection); no Eclipse types appear here.
 *
 * <p>{@link #check(String, Path)} never throws and never returns
 * {@code null}: equal directories (trailing separators, case and separator
 * style ignored, per Windows conventions) are {@link Kind#OK}, two known but
 * different directories are {@link Kind#MISMATCH} carrying both paths,
 * anything absent on either side is {@link Kind#UNKNOWN}.
 * {@link #warningLine()} renders the mismatch as the marker line the view
 * prefixes to the header tooltip; empty otherwise.</p>
 */
public record CwdCheck(Kind kind, String serverPath, Path workspacePath) {

    /** The outcome of the cross-check; {@link #UNKNOWN} whenever a side is absent. */
    public enum Kind { OK, MISMATCH, UNKNOWN }

    /**
     * Compares the server's resolved project directory with the active
     * workspace project directory: ignoring case, trailing separators and
     * separator style ({@code \} vs {@code /} — the server and
     * {@link Path#toString()} may each use either), per Windows conventions;
     * {@code null}/blank on either side yields {@link Kind#UNKNOWN}.
     */
    public static CwdCheck check(String serverProjectPath, Path workspaceProjectDir) {
        String server = ProjectVcs.trimSeparators(serverProjectPath);
        if (server == null || workspaceProjectDir == null) {
            return new CwdCheck(Kind.UNKNOWN, server, workspaceProjectDir);
        }
        String workspace = ProjectVcs.trimSeparators(workspaceProjectDir.toString());
        if (workspace == null) {
            return new CwdCheck(Kind.UNKNOWN, server, workspaceProjectDir);
        }
        return new CwdCheck(sameDirectory(server, workspace) ? Kind.OK : Kind.MISMATCH, server, workspaceProjectDir);
    }

    private static boolean sameDirectory(String server, String workspace) {
        return server.replace('\\', '/').equalsIgnoreCase(workspace.replace('\\', '/'));
    }

    /**
     * The visible warning marker line for the header tooltip —
     * {@code "⚠ cwd mismatch: <server> vs <workspace>"} — empty unless the
     * check found a mismatch.
     */
    public String warningLine() {
        return kind == Kind.MISMATCH
                ? "⚠ cwd mismatch: " + serverPath + " vs " + workspacePath
                : "";
    }
}
