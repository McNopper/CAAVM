package com.opencode.ide.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.opencode.ide.client.model.OpencodeEvent;

/**
 * Unit tests for the pure SSE wire-format parser {@link Sse} - no server, no I/O.
 */
public class SseParsingTest {

    @Test
    public void parsesSingleEvent() {
        String sse = "data: {\"type\":\"session.created\",\"properties\":{\"info\":{\"id\":\"ses_1\"}}}\n\n";
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(1, events.size());
        assertEquals("session.created", events.get(0).type());
        assertEquals("ses_1", events.get(0).at("info.id"));
    }

    @Test
    public void parsesMultipleEvents() {
        String sse = """
                data: {"type":"session.status","properties":{"sessionID":"ses_a"}}

                data: {"type":"session.idle","properties":{"sessionID":"ses_b"}}

                """;
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(2, events.size());
        assertEquals("session.status", events.get(0).type());
        assertEquals("ses_a", events.get(0).string("sessionID"));
        assertEquals("session.idle", events.get(1).type());
    }

    @Test
    public void joinsMultiLineDataFrames() {
        String sse = "data: {\"type\":\"x\",\ndata: \"properties\":{\"k\":\"v\"}}\n\n";
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(1, events.size());
        assertEquals("x", events.get(0).type());
        // the event's `properties` field is the inner object, so "v" lives at key "k"
        assertEquals("v", events.get(0).at("k"));
    }

    @Test
    public void skipsMalformedFrames() {
        String sse = "data: {\"type\":\"ok\",\"properties\":{}}\n\ndata: this-is-not-json\n\ndata: {broken\n\n";
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(1, events.size());
        assertEquals("ok", events.get(0).type());
    }

    @Test
    public void ignoresNonDataLines() {
        String sse = """
                : comment line
                event: session.status
                data: {"type":"session.status","properties":{"sessionID":"ses_x"}}

                """;
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(1, events.size());
        assertEquals("ses_x", events.get(0).string("sessionID"));
    }

    @Test
    public void parseEventNullForBlank() {
        assertNull(Sse.parseEvent(null));
        assertNull(Sse.parseEvent(""));
        assertNull(Sse.parseEvent("   "));
    }

    @Test
    public void emptyInputYieldsNoEvents() {
        assertTrue(Sse.events("").isEmpty());
        assertTrue(Sse.events(null).isEmpty());
    }

    @Test
    public void unterminatedFrameAtEndOfInputIsEmitted() {
        String sse = "data: {\"type\":\"session.idle\",\"properties\":{\"sessionID\":\"ses_z\"}}";
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(1, events.size());
        assertEquals("session.idle", events.get(0).type());
        assertEquals("ses_z", events.get(0).string("sessionID"));
    }

    @Test
    public void unterminatedMultiLineFrameAtEndOfInputIsEmitted() {
        String sse = "data: {\"type\":\"x\",\ndata: \"properties\":{\"k\":\"v\"}}";
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(1, events.size());
        assertEquals("x", events.get(0).type());
        assertEquals("v", events.get(0).at("k"));
    }

    @Test
    public void terminatedThenUnterminatedFramesBothEmitted() {
        String sse = "data: {\"type\":\"a\",\"properties\":{}}\n\ndata: {\"type\":\"b\",\"properties\":{}}";
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(2, events.size());
        assertEquals("a", events.get(0).type());
        assertEquals("b", events.get(1).type());
    }

    @Test
    public void partialDataAtEndOfInputIsFlushedButSkippedWhenMalformed() {
        String partial = "data: {\"type\":";
        List<String> frames = Sse.frames(java.util.Arrays.asList(partial).iterator());
        assertEquals("EOF must flush pending data as a frame", 1, frames.size());
        assertEquals("{\"type\":", frames.get(0));
        assertTrue("malformed flushed frame is skipped downstream",
                Sse.events(partial).isEmpty());
    }

    @Test
    public void noDataLinesAtEndOfInputEmitNothing() {
        List<String> frames = Sse.frames(java.util.Arrays.asList(": comment", "event: x").iterator());
        assertTrue(frames.isEmpty());
    }

    @Test
    public void unwrapsGlobalEventPayloadEnvelope() {
        // /global/event frame shape: {"directory":…, "project":…, "payload":{"id","type","properties"}}
        String sse = "data: {\"directory\":\"/repo\",\"project\":\"p1\","
                + "\"payload\":{\"id\":\"evt_1\",\"type\":\"session.created\",\"properties\":{\"info\":{\"id\":\"ses_g1\"}}}}\n\n";
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(1, events.size());
        assertEquals("session.created", events.get(0).type());
        assertEquals("ses_g1", events.get(0).at("info.id"));
    }

    @Test
    public void plainFramesWithoutEnvelopeStillParse() {
        String sse = "data: {\"type\":\"session.idle\",\"properties\":{\"sessionID\":\"ses_1\"}}\n\n";
        List<OpencodeEvent> events = Sse.events(sse);
        assertEquals(1, events.size());
        assertEquals("session.idle", events.get(0).type());
        assertEquals("ses_1", events.get(0).string("sessionID"));
    }
}
