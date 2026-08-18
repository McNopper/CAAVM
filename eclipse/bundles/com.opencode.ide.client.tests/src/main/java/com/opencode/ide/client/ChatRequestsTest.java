package com.opencode.ide.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonParser;

/**
 * Unit tests for {@link ChatRequests} request-body building (pure, no I/O).
 *
 * <p>Field shapes are those of {@code POST /session/:id/message} in the live
 * server's OpenAPI ({@code model{providerID,modelID}}, {@code variant},
 * {@code system}, {@code parts[]}).</p>
 */
public class ChatRequestsTest {

    private static ChatRequest request(String agent, String provider, String model, String text) {
        return new ChatRequest("ses_1", agent, provider, model, null, null, text);
    }

    @Test
    public void bodyContainsAgentAndTextPart() {
        String json = ChatRequests.messageBody(request("build", null, null, "hello **world**"));
        var obj = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("build", obj.get("agent").getAsString());
        assertFalse(obj.has("model")); // default model -> omitted
        var parts = obj.getAsJsonArray("parts");
        assertEquals(1, parts.size());
        var part = parts.get(0).getAsJsonObject();
        assertEquals("text", part.get("type").getAsString());
        assertEquals("hello **world**", part.get("text").getAsString());
    }

    @Test
    public void modelIsIncludedWhenProviderAndModelSet() {
        String json = ChatRequests.messageBody(request("build", "opencode", "glm-5.2", "hi"));
        var obj = JsonParser.parseString(json).getAsJsonObject();
        var model = obj.getAsJsonObject("model");
        assertEquals("opencode", model.get("providerID").getAsString());
        assertEquals("glm-5.2", model.get("modelID").getAsString());
    }

    @Test
    public void halfSetModelIsOmitted() {
        assertFalse(JsonParser.parseString(ChatRequests.messageBody(
                request("build", "opencode", null, "hi"))).getAsJsonObject().has("model"));
        assertFalse(JsonParser.parseString(ChatRequests.messageBody(
                request("build", null, "glm-5.2", "hi"))).getAsJsonObject().has("model"));
    }

    @Test
    public void variantIsSentAsATopLevelFieldWhenSelected() {
        String json = ChatRequests.messageBody(
                request("build", "opencode-go", "gpt-5.6-luna", "hi").withVariant("high"));
        var obj = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("high", obj.get("variant").getAsString());
        // the variant must NOT be folded into the model object
        assertFalse(obj.getAsJsonObject("model").has("variant"));
    }

    @Test
    public void variantIsOmittedWhenNullOrBlank() {
        assertFalse(JsonParser.parseString(ChatRequests.messageBody(
                request("build", "p", "m", "hi").withVariant(null))).getAsJsonObject().has("variant"));
        assertFalse(JsonParser.parseString(ChatRequests.messageBody(
                request("build", "p", "m", "hi").withVariant("  "))).getAsJsonObject().has("variant"));
    }

    @Test
    public void systemPromptIsSentWhenSet() {
        String json = ChatRequests.messageBody(
                request("build", null, null, "hi").withSystem(ChatCapabilities.RENDERER_SYSTEM_PROMPT));
        var obj = JsonParser.parseString(json).getAsJsonObject();
        String system = obj.get("system").getAsString();
        assertTrue(system.contains("KaTeX") || system.contains("$$"));
        assertTrue("must advertise language-tagged code fences", system.contains("```cpp"));
        assertTrue("must advertise mermaid", system.contains("mermaid"));
    }

    @Test
    public void systemPromptIsOmittedWhenNull() {
        assertFalse(JsonParser.parseString(ChatRequests.messageBody(
                request("build", null, null, "hi"))).getAsJsonObject().has("system"));
    }

    @Test
    public void nullTextBecomesEmptyAndNewlinesSurvive() {
        String json = ChatRequests.messageBody(request("build", null, null, "line1\nline2 \"quoted\" <b>"));
        var part = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject();
        assertEquals("line1\nline2 \"quoted\" <b>", part.get("text").getAsString());
        String jsonNull = ChatRequests.messageBody(request("build", null, null, null));
        var partNull = JsonParser.parseString(jsonNull).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject();
        assertEquals("", partNull.get("text").getAsString());
    }

    @Test
    public void withersDoNotMutateTheOriginalRequest() {
        ChatRequest base = ChatRequest.of("ses_1", "hi");
        ChatRequest derived = base.withAgent("build").withModel("p", "m").withVariant("max");
        assertEquals(null, base.agent());
        assertFalse(base.hasModel());
        assertEquals("build", derived.agent());
        assertTrue(derived.hasModel());
        assertEquals("max", derived.variant());
        assertEquals("hi", derived.text());
    }
}
