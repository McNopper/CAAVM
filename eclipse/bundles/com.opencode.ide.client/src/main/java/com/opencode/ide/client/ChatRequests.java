package com.opencode.ide.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Builds opencode chat request bodies (pure JSON building, no I/O - unit-testable).
 * Used by {@code HttpOpencodeClient} for {@code POST /session/:id/message}.
 */
public final class ChatRequests {

    private ChatRequests() {
    }

    /**
     * @param request the chat request (see {@link ChatRequest})
     * @return the JSON request body
     */
    public static String messageBody(ChatRequest request) {
        JsonObject body = new JsonObject();
        if (isSet(request.agent())) {
            body.addProperty("agent", request.agent());
        }
        if (request.hasModel()) {
            JsonObject model = new JsonObject();
            model.addProperty("providerID", request.providerId());
            model.addProperty("modelID", request.modelId());
            body.add("model", model);
        }
        // reasoning-effort variant (e.g. "high", "thinking"); omitted = model default
        if (isSet(request.variant())) {
            body.addProperty("variant", request.variant());
        }
        // per-request system prompt (client capability advertisement)
        if (isSet(request.system())) {
            body.addProperty("system", request.system());
        }
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("type", "text");
        part.addProperty("text", request.text() == null ? "" : request.text());
        parts.add(part);
        body.add("parts", parts);
        return body.toString();
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
