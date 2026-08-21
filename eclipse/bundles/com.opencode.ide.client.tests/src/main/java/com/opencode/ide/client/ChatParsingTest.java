package com.opencode.ide.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.ChatMessageInfo;
import com.opencode.ide.client.model.ChatPart;

/**
 * Unit tests for the {@code GET /session/:id/message} mapping.
 *
 * <p>The fixture below is a <b>verbatim capture from a live opencode server</b>
 * (v1.18.18). It must stay that way: an earlier hand-written fixture invented a
 * nested {@code "model"} object on the <i>assistant</i> info, so the tests passed
 * while the real (flat {@code providerID}/{@code modelID}) shape left the model
 * label empty in the UI.</p>
 */
public class ChatParsingTest {

    private static final Gson GSON = new Gson();

    /** Captured from GET /session/:id/message (opencode 1.18.18). */
    private static final String HISTORY_JSON = """
            [
              {
                "info": {
                  "id": "msg_0036e089d001Fqti0Zp8VVfjbY",
                  "sessionID": "ses_ffc91f780ffeP6jS7mm7yDCK7a",
                  "role": "user",
                  "time": { "created": 1786763937949 },
                  "summary": { "diffs": [] },
                  "agent": "build",
                  "model": { "providerID": "opencode-go", "modelID": "kimi-k2.7-code" }
                },
                "parts": [
                  { "id": "prt_1", "sessionID": "ses_ffc91f780ffeP6jS7mm7yDCK7a",
                    "messageID": "msg_0036e089d001Fqti0Zp8VVfjbY",
                    "type": "text", "text": "Reply with exactly: ack" }
                ]
              },
              {
                "info": {
                  "id": "msg_0036e08bc001ps0jHM7NO3caut",
                  "sessionID": "ses_ffc91f780ffeP6jS7mm7yDCK7a",
                  "role": "assistant",
                  "time": { "created": 1786763937980, "completed": 1786763947855 },
                  "parentID": "msg_0036e089d001Fqti0Zp8VVfjbY",
                  "modelID": "kimi-k2.7-code",
                  "providerID": "opencode-go",
                  "mode": "build",
                  "agent": "build",
                  "path": { "cwd": "C:\\\\Temp\\\\IDE", "root": "/" },
                  "cost": 0.0064992,
                  "tokens": { "total": 6761, "input": 6736, "output": 3, "reasoning": 22,
                              "cache": { "read": 0, "write": 0 } },
                  "finish": "stop"
                },
                "parts": [
                  { "id": "prt_2", "type": "step-start" },
                  { "id": "prt_3", "type": "reasoning",
                    "text": "The user wants a simple \\"ack\\" reply.",
                    "time": { "start": 1786763941171, "end": 1786763941703 } },
                  { "id": "prt_4", "type": "text", "text": "ack",
                    "time": { "start": 1786763941706, "end": 1786763947844 } },
                  { "id": "prt_5", "type": "step-finish", "reason": "stop", "cost": 0.0064992 }
                ]
              }
            ]
            """;

    @Test
    public void historyMapsRolesAndParts() {
        List<ChatEntry> entries = GSON.fromJson(HISTORY_JSON,
                TypeToken.getParameterized(List.class, ChatEntry.class).getType());

        assertEquals(2, entries.size());

        ChatEntry user = entries.get(0);
        assertTrue(user.isUser());
        assertEquals("msg_0036e089d001Fqti0Zp8VVfjbY", user.info().id());
        assertEquals("user", user.info().role());
        assertEquals("Reply with exactly: ack", user.text());
        assertEquals("", user.reasoning());

        ChatEntry assistant = entries.get(1);
        assertTrue(!assistant.isUser());
        assertEquals("assistant", assistant.info().role());
        assertEquals("ack", assistant.text());
        assertEquals("The user wants a simple \"ack\" reply.", assistant.reasoning());
    }

    @Test
    public void assistantModelIsReadFromTheFlatFields() {
        // real assistant shape: providerID/modelID directly on info (no nested "model")
        List<ChatEntry> entries = GSON.fromJson(HISTORY_JSON,
                TypeToken.getParameterized(List.class, ChatEntry.class).getType());
        ChatMessageInfo info = entries.get(1).info();

        assertNull("live assistant payloads have no nested model object", info.model());
        assertEquals("opencode-go", info.providerId());
        assertEquals("kimi-k2.7-code", info.modelId());
        assertEquals("opencode-go/kimi-k2.7-code", info.modelLabel());
        assertEquals("stop", info.finish());
        assertEquals("build", info.mode());
    }

    @Test
    public void userModelIsReadFromTheNestedObject() {
        List<ChatEntry> entries = GSON.fromJson(HISTORY_JSON,
                TypeToken.getParameterized(List.class, ChatEntry.class).getType());
        ChatMessageInfo info = entries.get(0).info();

        assertEquals("opencode-go", info.providerId());
        assertEquals("kimi-k2.7-code", info.modelId());
        assertEquals("opencode-go/kimi-k2.7-code", info.modelLabel());
    }

    @Test
    public void modelLabelIsEmptyWhenTheServerOmitsTheModel() {
        ChatEntry entry = GSON.fromJson("{\"info\":{\"id\":\"m\",\"role\":\"assistant\"},\"parts\":[]}",
                ChatEntry.class);
        assertEquals("", entry.info().modelLabel());
        assertNull(entry.info().providerId());
    }

    @Test
    public void nullPartsAreTolerated() {
        ChatEntry entry = GSON.fromJson("{\"info\":{\"id\":\"m\",\"role\":\"user\"},\"parts\":null}", ChatEntry.class);
        assertEquals("", entry.text());
        assertEquals(0, entry.parts().size());
    }

    @Test
    public void missingInfoFieldsAreNull() {
        ChatEntry entry = GSON.fromJson("{\"info\":{\"id\":\"m\"},\"parts\":[]}", ChatEntry.class);
        assertNull(entry.info().role());
        assertNull(entry.info().model());
    }

    /**
     * Verbatim-style capture of tool parts (opencode 1.18.x): {@code state} is an
     * OBJECT on the wire ({@code state.status} = running/completed/error) — never a
     * string. Regression test: a string-typed {@code state} field made Gson fail the
     * whole getMessages/sendMessage response with "malformed response body" whenever
     * a session contained a tool part.
     */
    private static final String TOOL_PARTS_JSON = """
            [
              {
                "info": {
                  "id": "msg_tool_1", "sessionID": "ses_tool", "role": "assistant",
                  "time": { "created": 1786764000000, "completed": 1786764009000 },
                  "modelID": "kimi-k2.7-code", "providerID": "opencode-go",
                  "mode": "build", "agent": "build", "finish": "stop"
                },
                "parts": [
                  { "id": "prt_t0", "type": "step-start" },
                  { "id": "prt_t1", "type": "tool", "tool": "read",
                    "state": { "status": "completed",
                               "input": { "filePath": "src/main.cpp" },
                               "output": "int main() { return 0; }",
                               "time": { "start": 1786764001000, "end": 1786764002000 } } },
                  { "id": "prt_t2", "type": "tool", "tool": "bash",
                    "state": { "status": "running",
                               "input": { "command": "cmake --build build" },
                               "time": { "start": 1786764003000 } } },
                  { "id": "prt_t3", "type": "tool", "tool": "grep",
                    "state": { "status": "error",
                               "input": { "pattern": "TODO" },
                               "error": "no matches",
                               "time": { "start": 1786764004000, "end": 1786764005000 } } },
                  { "id": "prt_t4", "type": "text", "text": "done", "time": { "start": 1786764008000 } },
                  { "id": "prt_t5", "type": "step-finish", "reason": "stop" }
                ]
              }
            ]
            """;

    @Test
    public void toolPartsParseWithNestedStateStatus() {
        List<ChatEntry> entries = GSON.fromJson(TOOL_PARTS_JSON,
                TypeToken.getParameterized(List.class, ChatEntry.class).getType());
        assertEquals(1, entries.size());

        var tools = entries.get(0).parts().stream().filter(ChatPart::isTool).toList();
        assertEquals(3, tools.size());
        assertEquals("read", tools.get(0).tool());
        assertEquals("completed", tools.get(0).stateName());
        assertEquals("bash", tools.get(1).tool());
        assertEquals("running", tools.get(1).stateName());
        assertEquals("grep", tools.get(2).tool());
        assertEquals("error", tools.get(2).stateName());
        // text part untouched by the state mapping
        assertEquals("done", entries.get(0).text());
    }

    @Test
    public void toolPartWithoutStateIsNullStateName() {
        ChatEntry entry = GSON.fromJson(
                "{\"info\":{\"id\":\"m\",\"role\":\"assistant\"},\"parts\":[{\"type\":\"tool\",\"tool\":\"read\"}]}",
                ChatEntry.class);
        ChatPart part = entry.parts().get(0);
        assertTrue(part.isTool());
        assertNull(part.stateName());
    }
}
