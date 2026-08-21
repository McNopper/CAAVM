package com.opencode.ide.board.fleet;

/**
 * Seam between the Board view ("Launch task") and the fleet engine. The real
 * implementation will delegate to the {@code TaskFleet} task-driven launcher
 * in {@code com.opencode.ide.fleet}; until that wiring lands, the board runs
 * on {@link DefaultFleetLauncher}.
 */
public interface FleetLauncher {

    /**
     * Launches an agent on the given ticket.
     *
     * @param project  the task-store project id
     * @param ticketId the ticket id (e.g. {@code H2-001})
     * @return the resulting job handle (never {@code null}; a launch failure
     *         comes back as a {@code FAILED} handle with the reason in
     *         {@code detail}, never as an exception)
     */
    FleetJobHandle launch(String project, String ticketId);
}
