package com.opencode.ide.client.model;

/**
 * The {@code info} object of a chat message entry from
 * {@code GET /session/:id/message} (v1.18.x shape, captured from a live server).
 *
 * <p>The two roles carry the model differently - this was verified against real
 * payloads, do not "simplify" it:</p>
 * <ul>
 *   <li><b>user</b>: nested {@code "model": {"providerID": …, "modelID": …}}</li>
 *   <li><b>assistant</b>: <em>flat</em> {@code "providerID"} / {@code "modelID"}
 *       (plus {@code mode}, {@code cost}, {@code tokens}, {@code finish})</li>
 * </ul>
 *
 * <p>Use {@link #providerId()} / {@link #modelId()} instead of the raw fields so
 * both shapes resolve.</p>
 */
public record ChatMessageInfo(
        String id,
        String sessionID,
        String role,
        Session.Time time,
        String agent,
        String mode,
        String finish,
        Double cost,
        Session.Tokens tokens,
        String providerID,
        String modelID,
        String variant,
        Agent.ModelRef model) {

    /** Provider id for either role shape ({@code null} when the server omits it). */
    public String providerId() {
        if (providerID != null && !providerID.isBlank()) {
            return providerID;
        }
        return (model != null) ? model.providerID() : null;
    }

    /** Model id for either role shape ({@code null} when the server omits it). */
    public String modelId() {
        if (modelID != null && !modelID.isBlank()) {
            return modelID;
        }
        return (model != null) ? model.modelID() : null;
    }

    /** Reasoning-effort variant used for this message, or {@code null}. */
    public String variantName() {
        if (variant != null && !variant.isBlank()) {
            return variant;
        }
        return (model != null && model.variant() != null && !model.variant().isBlank())
                ? model.variant() : null;
    }

    /**
     * @return {@code provider/model} (plus {@code " (variant)"} when a reasoning
     *         variant was used), or {@code ""} when unknown - the chat meta line.
     */
    public String modelLabel() {
        String provider = providerId();
        String modelName = modelId();
        if (provider == null || modelName == null) {
            return "";
        }
        String label = provider + "/" + modelName;
        String variantName = variantName();
        return (variantName == null) ? label : label + " (" + variantName + ")";
    }
}
