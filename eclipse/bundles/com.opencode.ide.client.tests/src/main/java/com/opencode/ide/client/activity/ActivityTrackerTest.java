package com.opencode.ide.client.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.Gson;
import com.opencode.ide.client.model.OpencodeEvent;

/**
 * Unit tests for {@link ActivityTracker}'s event-to-activity derivation
 * rules (session.status/idle/deleted, message.part.updated tool/reasoning).
 */
public class ActivityTrackerTest {

    private static final Gson GSON = new Gson();

    private static OpencodeEvent event(String json) {
        return GSON.fromJson(json, OpencodeEvent.class);
    }

    private static String statusEvent(String sessionId, String status) {
        return """
                {"type":"session.status","properties":{"sessionID":"%s","status":"%s"}}""".formatted(sessionId, status);
    }

    private static String idleEvent(String sessionId) {
        return """
                {"type":"session.idle","properties":{"sessionID":"%s"}}""".formatted(sessionId);
    }

    private static String deletedEvent(String sessionId) {
        return """
                {"type":"session.deleted","properties":{"sessionID":"%s"}}""".formatted(sessionId);
    }

    private static String partEvent(String sessionId, String partType) {
        return """
                {"type":"message.part.updated","properties":{"sessionID":"%s","part":{"type":"%s"}}}""".formatted(sessionId,
                partType);
    }

    private static String toolPartEvent(String sessionId, String tool, String status, String inputJson) {
        String state = status == null ? "" : "\"state\":{\"status\":\"" + status + "\"},";
        return """
                {"type":"message.part.updated","properties":{"sessionID":"%s","part":{"type":"tool","tool":"%s",%s"input":%s}}}"""
                .formatted(sessionId, tool, state, inputJson);
    }

    @Test
    public void statusBusyAndRetryMarkSessionRunning() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(statusEvent("ses_1", "busy")));
        tracker.apply(event(statusEvent("ses_2", "retry")));
        ActivitySnapshot snapshot = tracker.snapshot();
        assertEquals(2, snapshot.sessions().size());
        assertTrue(snapshot.sessions().get("ses_1").running());
        assertTrue(snapshot.sessions().get("ses_2").running());
        assertFalse(snapshot.sessions().get("ses_1").thinking());
    }

    @Test
    public void statusIdleStopsRunningSession() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(statusEvent("ses_1", "busy")));
        tracker.apply(event(statusEvent("ses_1", "idle")));
        ActivitySnapshot snapshot = tracker.snapshot();
        assertEquals(1, snapshot.sessions().size());
        assertFalse(snapshot.sessions().get("ses_1").running());
    }

    @Test
    public void statusIdleForUnknownSessionCreatesNothing() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(statusEvent("ses_1", "idle")));
        tracker.apply(event(statusEvent("ses_2", "waiting")));
        assertTrue(tracker.snapshot().sessions().isEmpty());
    }

    @Test
    public void idleEventStopsRunningSession() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(statusEvent("ses_1", "busy")));
        tracker.apply(event(idleEvent("ses_1")));
        assertFalse(tracker.snapshot().sessions().get("ses_1").running());
    }

    @Test
    public void toolRunningWithFileAppearsInSnapshot() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(statusEvent("ses_1", "busy")));
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"filePath\":\"/src/A.java\"}")));
        ActivitySnapshot snapshot = tracker.snapshot();
        FileActivity file = snapshot.files().get("/src/A.java");
        assertNotNull(file);
        assertEquals("ses_1", file.sessionId());
        assertEquals("edit", file.tool());
        assertEquals("/src/A.java", file.file());
        SessionActivity session = snapshot.sessions().get("ses_1");
        assertEquals(1, session.activity().size());
        ToolActivity tool = session.activity().get(0);
        assertEquals("edit", tool.tool());
        assertEquals("/src/A.java", tool.file());
        assertEquals(ToolActivity.State.RUNNING, tool.state());
    }

    @Test
    public void toolCompletedRemovesFileAndShowsCompleted() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"filePath\":\"/src/A.java\"}")));
        tracker.apply(event(toolPartEvent("ses_1", "edit", "completed", "{\"filePath\":\"/src/A.java\"}")));
        ActivitySnapshot snapshot = tracker.snapshot();
        assertNull(snapshot.files().get("/src/A.java"));
        assertEquals(1, snapshot.sessions().get("ses_1").activity().size());
        assertEquals(ToolActivity.State.COMPLETED, snapshot.sessions().get("ses_1").activity().get(0).state());
    }

    @Test
    public void toolErrorWithoutFileRecordsErrorState() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(toolPartEvent("ses_1", "bash", "error", "{\"command\":\"ls\"}")));
        ActivitySnapshot snapshot = tracker.snapshot();
        assertTrue(snapshot.files().isEmpty());
        SessionActivity session = snapshot.sessions().get("ses_1");
        assertEquals(1, session.activity().size());
        ToolActivity tool = session.activity().get(0);
        assertEquals("bash", tool.tool());
        assertNull(tool.file());
        assertEquals(ToolActivity.State.ERROR, tool.state());
    }

    @Test
    public void missingToolStateDefaultsToRunning() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(toolPartEvent("ses_1", "edit", null, "{\"filePath\":\"/src/B.java\"}")));
        ActivitySnapshot snapshot = tracker.snapshot();
        assertNotNull(snapshot.files().get("/src/B.java"));
        assertEquals(ToolActivity.State.RUNNING, snapshot.sessions().get("ses_1").activity().get(0).state());
    }

    @Test
    public void filePathVariantKeysResolve() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"path\":\"/p\"}")));
        tracker.apply(event(toolPartEvent("ses_1", "read", "running", "{\"file\":\"/f\"}")));
        tracker.apply(event(toolPartEvent("ses_1", "glob", "running", "{\"absolutePath\":\"/ap\"}")));
        ActivitySnapshot snapshot = tracker.snapshot();
        assertEquals(3, snapshot.files().size());
        assertNotNull(snapshot.files().get("/p"));
        assertNotNull(snapshot.files().get("/f"));
        assertNotNull(snapshot.files().get("/ap"));
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"filePath\":\"/first\",\"path\":\"/second\"}")));
        assertEquals("edit", tracker.snapshot().files().get("/first").tool());
        assertEquals(4, tracker.snapshot().files().size());
    }

    @Test
    public void reasoningThenTextTogglesThinking() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(partEvent("ses_1", "reasoning")));
        assertTrue(tracker.snapshot().sessions().get("ses_1").thinking());
        tracker.apply(event(partEvent("ses_1", "text")));
        assertFalse(tracker.snapshot().sessions().get("ses_1").thinking());
    }

    @Test
    public void sessionDeletedDropsSessionAndFiles() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"filePath\":\"/a\"}")));
        tracker.apply(event(toolPartEvent("ses_2", "edit", "running", "{\"filePath\":\"/b\"}")));
        tracker.apply(event(deletedEvent("ses_1")));
        ActivitySnapshot snapshot = tracker.snapshot();
        assertNull(snapshot.sessions().get("ses_1"));
        assertNotNull(snapshot.sessions().get("ses_2"));
        assertNull(snapshot.files().get("/a"));
        assertNotNull(snapshot.files().get("/b"));
    }

    @Test
    public void sessionEndedDropsSessionAndFiles() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"filePath\":\"/a\"}")));
        tracker.sessionEnded("ses_1");
        ActivitySnapshot snapshot = tracker.snapshot();
        assertTrue(snapshot.sessions().isEmpty());
        assertTrue(snapshot.files().isEmpty());
        tracker.sessionEnded("unknown");
        assertTrue(tracker.snapshot().sessions().isEmpty());
        assertTrue(tracker.snapshot().files().isEmpty());
    }

    @Test
    public void listenerFiresOnlyOnActualChange() {
        ActivityTracker tracker = new ActivityTracker();
        int[] calls = { 0 };
        tracker.addListener(() -> calls[0]++);
        tracker.apply(event(statusEvent("ses_1", "busy")));
        assertEquals(1, calls[0]);
        tracker.apply(event(statusEvent("ses_1", "busy")));
        tracker.apply(event(statusEvent("ses_1", "retry")));
        assertEquals(1, calls[0]);
        tracker.apply(event(statusEvent("ses_1", "idle")));
        assertEquals(2, calls[0]);
        tracker.apply(event(statusEvent("ses_1", "idle")));
        tracker.apply(event(idleEvent("ses_1")));
        tracker.apply(event("{\"type\":\"todo.updated\",\"properties\":{}}"));
        tracker.apply(null);
        assertEquals(2, calls[0]);
    }

    @Test
    public void duplicateToolUpdateDoesNotRefire() {
        ActivityTracker tracker = new ActivityTracker();
        int[] calls = { 0 };
        tracker.addListener(() -> calls[0]++);
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"filePath\":\"/a\"}")));
        assertEquals(1, calls[0]);
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"filePath\":\"/a\"}")));
        assertEquals(1, calls[0]);
        tracker.apply(event(toolPartEvent("ses_1", "edit", "completed", "{\"filePath\":\"/a\"}")));
        assertEquals(2, calls[0]);
        tracker.apply(event(toolPartEvent("ses_1", "edit", "completed", "{\"filePath\":\"/a\"}")));
        assertEquals(2, calls[0]);
    }

    @Test
    public void snapshotIsImmutable() {
        ActivityTracker tracker = new ActivityTracker();
        tracker.apply(event(toolPartEvent("ses_1", "edit", "running", "{\"filePath\":\"/a\"}")));
        ActivitySnapshot snapshot = tracker.snapshot();
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.sessions().put("ses_2", snapshot.sessions().get("ses_1")));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.files().remove("/a"));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.sessions().get("ses_1").activity().add(new ToolActivity("read", null, null)));
        assertNotNull(tracker.snapshot().files().get("/a"));
        assertEquals(1, tracker.snapshot().sessions().get("ses_1").activity().size());
    }

    @Test
    public void nullAndUnknownEventsAreIgnored() {
        ActivityTracker tracker = new ActivityTracker();
        int[] calls = { 0 };
        tracker.addListener(() -> calls[0]++);
        tracker.apply(null);
        tracker.apply(event("{\"type\":\"todo.updated\",\"properties\":{}}"));
        tracker.apply(event("{\"type\":\"session.status\",\"properties\":{}}"));
        tracker.apply(event(partEvent("ses_9", "text")));
        tracker.apply(event("{\"type\":\"message.part.updated\",\"properties\":{\"sessionID\":\"ses_9\"}}"));
        assertTrue(tracker.snapshot().sessions().isEmpty());
        assertTrue(tracker.snapshot().files().isEmpty());
        assertEquals(0, calls[0]);
    }
}
