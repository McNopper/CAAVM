package com.opencode.ide.ui.session;

import com.opencode.ide.client.model.OpencodeEvent;

/**
 * SWT-free predicate deciding whether one {@link OpencodeEvent} from the
 * {@code /event} SSE stream is relevant for the Session Details view of one
 * session: the event type must change the transcript/history, and the event
 * must belong to THIS session id.
 *
 * <p>Tolerates malformed events: {@code null} event, {@code null} type,
 * missing properties, or a session id that cannot be located all yield
 * {@code false} (never an exception, never a refresh of unrelated views).</p>
 */
public final class SessionEventFilter {

    private SessionEventFilter() {
    }

    /**
     * @param sessionId the view's session id (the secondary id it was opened with)
     * @param event     the live server event (may be {@code null}/malformed)
     * @return {@code true} when the event changes this session's history and a
     *         (debounced) reload is warranted
     */
    public static boolean shouldRefreshFor(String sessionId, OpencodeEvent event) {
        if (sessionId == null || sessionId.isBlank() || event == null) {
            return false;
        }
        if (event.type() == null) {
            return false;
        }
        return switch (event.type()) {
            // session.idle: turn finished -> final state; session.updated: title/metadata
            // changed; message.updated: a message appeared/changed; message.part.updated:
            // streaming part progress (arrives in bursts, callers must coalesce).
            case "session.idle", "session.updated", "message.updated", "message.part.updated" ->
                sessionId.equals(sessionIdOf(event));
            default -> false;
        };
    }

    /**
     * @return the session id an event belongs to, tolerating the shapes seen in
     *         the wild: a top-level {@code sessionID} (session.idle,
     *         message.updated, sometimes message.part.updated), a nested
     *         {@code part.sessionID} (message.part.updated), or {@code info.id}
     *         (session.updated); {@code null} when none is a matching string.
     */
    private static String sessionIdOf(OpencodeEvent event) {
        String id = event.string("sessionID");
        if (id == null) {
            id = event.at("part.sessionID");
        }
        if (id == null) {
            id = event.at("info.id");
        }
        return id;
    }
}
