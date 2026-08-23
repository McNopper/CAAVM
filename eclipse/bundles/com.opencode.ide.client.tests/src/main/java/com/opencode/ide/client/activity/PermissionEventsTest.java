package com.opencode.ide.client.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.Gson;
import com.opencode.ide.client.model.OpencodeEvent;

/**
 * Unit tests for {@link PermissionEvents}: the verified v1.18
 * {@code permission.asked}/{@code permission.replied} payload shapes, the
 * lenient fallbacks (alternate id keys, metadata title probing, pattern
 * filtering), and null-tolerance for foreign/malformed events.
 */
public class PermissionEventsTest {

    private static final Gson GSON = new Gson();

    private static OpencodeEvent event(String json) {
        return GSON.fromJson(json, OpencodeEvent.class);
    }

    @Test
    public void askedEventParsesIdsCategoryPatternsAndMetadataTitle() {
        PermissionRequest request = PermissionEvents.parse(event("""
                {"type":"permission.asked","properties":{
                  "id":"per_1","sessionID":"ses_1","permission":"bash",
                  "patterns":["git push","git status"],
                  "metadata":{"command":"git push","cwd":"/repo"},
                  "always":["git*"],"tool":{"messageID":"msg_1","callID":"call_1"}}}"""));
        assertEquals("ses_1", request.sessionId());
        assertEquals("per_1", request.permissionId());
        assertEquals("bash", request.permission());
        assertEquals(java.util.List.of("git push", "git status"), request.patterns());
        assertEquals("metadata.command is the display title", "git push", request.title());
        assertEquals(PermissionRequest.Status.PENDING, request.status());
        assertTrue(request.pending());
    }

    @Test
    public void askedWithoutMetadataTitleUsesPatternTitleKeyAndSkipsNonStrings() {
        PermissionRequest request = PermissionEvents.parse(event("""
                {"type":"permission.asked","properties":{
                  "id":"per_2","sessionID":"ses_1","permission":"edit",
                  "patterns":[42,"src/A.java",true],
                  "metadata":{"path":"src/A.java","title":"  "}}}"""));
        // blank "title" is skipped, "path" is the next candidate
        assertEquals("src/A.java", request.title());
        // non-string pattern entries are dropped
        assertEquals(java.util.List.of("src/A.java"), request.patterns());
        assertEquals("edit: src/A.java", request.display());
    }

    @Test
    public void askedWithoutTitleOrPatternsDisplaysBareCategory() {
        PermissionRequest request = PermissionEvents.parse(event("""
                {"type":"permission.asked","properties":{
                  "id":"per_3","sessionID":"ses_2","permission":"webfetch"}}"""));
        assertNull(request.title());
        assertEquals(java.util.List.of(), request.patterns());
        assertEquals("webfetch", request.display());
    }

    @Test
    public void repliedEventParsesRequestIDAsAnswered() {
        PermissionRequest request = PermissionEvents.parse(event("""
                {"type":"permission.replied","properties":{
                  "sessionID":"ses_1","requestID":"per_1","reply":"once"}}"""));
        assertEquals("ses_1", request.sessionId());
        assertEquals("per_1", request.permissionId());
        assertEquals(PermissionRequest.Status.ANSWERED, request.status());
        assertEquals(false, request.pending());
    }

    @Test
    public void permissionEventsWithoutActionableIdsReturnNull() {
        assertNull(PermissionEvents.parse(event(
                "{\"type\":\"permission.asked\",\"properties\":{\"permission\":\"bash\"}}")));
        assertNull(PermissionEvents.parse(event(
                "{\"type\":\"permission.asked\",\"properties\":{\"id\":\"per_1\"}}")));
        assertNull(PermissionEvents.parse(event(
                "{\"type\":\"permission.replied\",\"properties\":{\"reply\":\"once\"}}")));
        assertNull(PermissionEvents.parse(event(
                "{\"type\":\"permission.asked\",\"properties\":{}}")));
    }

    @Test
    public void nonPermissionAndMalformedEventsReturnNull() {
        assertNull(PermissionEvents.parse(null));
        assertNull(PermissionEvents.parse(event("{\"type\":\"session.idle\",\"properties\":{\"sessionID\":\"ses_1\"}}")));
        assertNull(PermissionEvents.parse(event("{\"type\":\"message.part.updated\",\"properties\":{}}")));
        assertNull(PermissionEvents.parse(event("{\"type\":\"todo.updated\",\"properties\":{}}")));
        assertNull(PermissionEvents.parse(event("{\"type\":\"permission.updated\",\"properties\":{}}")));
    }

    @Test
    public void missingOrNullMetadataAndPatternsAreTolerated() {
        PermissionRequest request = PermissionEvents.parse(event("""
                {"type":"permission.asked","properties":{
                  "id":"per_4","sessionID":"ses_3",
                  "patterns":"not-an-array","metadata":"not-an-object"}}"""));
        assertEquals(java.util.List.of(), request.patterns());
        assertNull(request.title());
        assertNull(request.permission());
    }
}
