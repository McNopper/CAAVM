package com.opencode.ide.git;

import java.nio.file.Path;

import com.opencode.ide.git.internal.GitWorktreeManager;

/**
 * The fleet's git naming conventions, in one place: the branch and worktree
 * location every task gets. The git bundle owns these strings — callers (the
 * fleet engine, the board's launcher/diff, views showing worktree locations)
 * must never re-spell them.
 *
 * <p>Pure Java, no Eclipse/OSGi.</p>
 */
public final class FleetGit {

    private static final String FLEET_DIR = ".git/opencode-fleet";
    private static final String BRANCH_PREFIX = "opencode/";

    private FleetGit() {
    }

    /** The fleet branch for a task: {@code opencode/<taskId>}. */
    public static String branchFor(String taskId) {
        return BRANCH_PREFIX + taskId;
    }

    /** The fleet worktree root: {@code <repoRoot>/.git/opencode-fleet}. */
    public static Path fleetRoot(Path repoRoot) {
        return repoRoot.resolve(FLEET_DIR);
    }

    /** The task's worktree location: {@code <repoRoot>/.git/opencode-fleet/<taskId>}. */
    public static Path worktreePath(Path repoRoot, String taskId) {
        return fleetRoot(repoRoot).resolve(taskId);
    }

    /** The default {@link WorktreeManager} implementation (git CLI backed). */
    public static WorktreeManager defaultManager() {
        return new GitWorktreeManager();
    }
}
