package com.opencode.ide.ui.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.opencode.ide.client.Sse;
import com.opencode.ide.client.model.OpencodeEvent;

/**
 * Unit tests for {@link SessionEventFilter}: the SWT-free predicate that
 * decides whether one SSE event should refresh the Session Details view of a
 * given session. No SWT, no Display — events are parsed from the real wire
 * format via {@link Sse#parseEvent(String)}, including malformed ones.
 */
public class SessionEventFilterTest {

    private static final String OWN = "ses_own";
    private static final String OTHER = "ses_other";

    /** One SSE JSON frame, exactly as the /event stream delivers it. */
    private static OpencodeEvent event(String json) {
        return Sse.parseEvent(json);
    }

    // ---------- own-session events trigger ----------

    @Test
    public void sessionIdleForOwnSessionTriggers() {
        assertTrue(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"session.idle\",\"properties\":{\"sessionID\":\"" + OWN + "\"}}")));
    }

    @Test
    public void messageUpdatedForOwnSessionTriggers() {
        assertTrue(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"message.updated\",\"properties\":{\"sessionID\":\"" + OWN + "\"}}")));
    }

    @Test
    public void sessionUpdatedForOwnSessionViaInfoIdTriggers() {
        assertTrue(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"session.updated\",\"properties\":{\"info\":{\"id\":\"" + OWN + "\"}}}")));
    }

    @Test
    public void messagePartUpdatedForOwnSessionViaTopLevelSessionIdTriggers() {
        assertTrue(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"message.part.updated\",\"properties\":{\"sessionID\":\"" + OWN + "\"}}")));
    }

    @Test
    public void messagePartUpdatedForOwnSessionViaPartSessionIdTriggers() {
        assertTrue(SessionEventFilter.shouldRefreshFor(OWN, event("{\"type\":\"message.part.updated\","
                + "\"properties\":{\"part\":{\"sessionID\":\"" + OWN + "\",\"type\":\"text\"}}}")));
    }

    // ---------- other sessions / irrelevant types are ignored ----------

    @Test
    public void messagePartUpdatedForOtherSessionIsIgnored() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"message.part.updated\",\"properties\":{\"sessionID\":\"" + OTHER + "\"}}")));
    }

    @Test
    public void messagePartUpdatedForOtherSessionViaPartSessionIdIsIgnored() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN, event("{\"type\":\"message.part.updated\","
                + "\"properties\":{\"part\":{\"sessionID\":\"" + OTHER + "\"}}}")));
    }

    @Test
    public void sessionIdleForOtherSessionIsIgnored() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"session.idle\",\"properties\":{\"sessionID\":\"" + OTHER + "\"}}")));
    }

    @Test
    public void irrelevantTypeForOwnSessionIsIgnored() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"todo.updated\",\"properties\":{\"sessionID\":\"" + OWN + "\"}}")));
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"session.created\",\"properties\":{\"sessionID\":\"" + OWN + "\"}}")));
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"session.deleted\",\"properties\":{\"sessionID\":\"" + OWN + "\"}}")));
    }

    // ---------- malformed input is tolerated ----------

    @Test
    public void nullEventYieldsFalse() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN, null));
    }

    @Test
    public void missingTypeYieldsFalse() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"properties\":{\"sessionID\":\"" + OWN + "\"}}")));
    }

    @Test
    public void nullPropertiesYieldFalse() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN, new OpencodeEvent("session.idle", null)));
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                new OpencodeEvent("message.part.updated", null)));
    }

    @Test
    public void emptyPropertiesYieldFalse() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"session.idle\",\"properties\":{}}")));
    }

    @Test
    public void nonStringSessionIdIsIgnored() {
        assertFalse(SessionEventFilter.shouldRefreshFor(OWN,
                event("{\"type\":\"session.idle\",\"properties\":{\"sessionID\":{\"id\":\"" + OWN + "\"}}}")));
    }

    // ---------- the queried session id itself is validated ----------

    @Test
    public void blankSessionIdYieldsFalse() {
        String json = "{\"type\":\"session.idle\",\"properties\":{\"sessionID\":\"" + OWN + "\"}}";
        assertFalse(SessionEventFilter.shouldRefreshFor(null, event(json)));
        assertFalse(SessionEventFilter.shouldRefreshFor("", event(json)));
        assertFalse(SessionEventFilter.shouldRefreshFor("  ", event(json)));
    }
}
