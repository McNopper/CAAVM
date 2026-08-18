package com.opencode.ide.git;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Gives each agent task its own git branch plus worktree and merges results
 * back into the main worktree. Merge-back is not synchronized internally;
 * callers (the fleet scheduler) must serialize it.
 */
public interface WorktreeManager {

    /**
     * Creates branch {@code opencode/<taskId>} and a worktree for it at
     * {@code repoRoot/.git/opencode-fleet/<taskId>} (hidden from
     * {@code git status}) starting at the current HEAD.
     */
    Worktree create(Path repoRoot, String taskId);

    /** Lists the fleet worktrees registered under {@code .git/opencode-fleet}. */
    List<Worktree> list(Path repoRoot);

    /** Finds the fleet worktree for a task, if any. */
    Optional<Worktree> find(Path repoRoot, String taskId);

    /** Removes the task's worktree and deletes its branch ({@code -D} when {@code force}). */
    void remove(Path repoRoot, String taskId, boolean force);

    /**
     * Merges the task branch into the current branch of the main worktree.
     * On conflict the merge is aborted and the conflicted file paths are
     * returned; the main worktree is never left in a merging state.
     */
    MergeResult mergeBack(Path repoRoot, String taskId);

    /** Reports branch existence, dirty file count and short HEAD sha of the task's worktree. */
    WorktreeStatus status(Path repoRoot, String taskId);
}
