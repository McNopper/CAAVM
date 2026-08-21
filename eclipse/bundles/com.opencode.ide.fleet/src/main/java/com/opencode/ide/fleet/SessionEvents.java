package com.opencode.ide.fleet;

import java.time.Duration;

import com.opencode.ide.client.OpencodeException;

/**
 * Awaits an agent session's next idle signal - the fleet's completion
 * detection seam. The default implementation polls the REST status endpoint
 * ({@link PollingSessionEvents}); {@link SseSessionEvents} replaces polling
 * with the server's {@code /event} SSE stream where one is available. The
 * seam is deliberately pure: idle means the session finished, whatever the
 * message history looks like - callers verify the reply separately (see
 * {@link FleetRunner#isComplete}).
 *
 * <p>Pure Java, no Eclipse/OSGi.</p>
 */
public interface SessionEvents {

    /**
     * Blocks until the session is (again) idle.
     *
     * @param sessionId the opencode session to watch
     * @param timeout   how long to wait
     * @return {@code true} when the session went idle (or was deleted) within
     *         the timeout, {@code false} on timeout
     * @throws OpencodeException when the underlying transport fails
     */
    boolean awaitIdle(String sessionId, Duration timeout) throws OpencodeException;
}
