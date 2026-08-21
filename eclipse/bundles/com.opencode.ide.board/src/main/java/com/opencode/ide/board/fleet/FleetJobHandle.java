package com.opencode.ide.board.fleet;

/**
 * The board's view of a fleet job: a SWT-free row value. Mirrors the fleet
 * engine's {@code FleetJob} shape (taskId, sessionId, worktree, state,
 * detail) without coupling the board to the engine bundle.
 */
public record FleetJobHandle(String taskId, String sessionId, String worktree, State state, String detail) {

    /** Lifecycle of a fleet job (mirrors the engine's states). */
    public enum State {
        RUNNING, COMPLETED, MERGED, FAILED
    }

    public boolean failed() {
        return state == State.FAILED;
    }
}
