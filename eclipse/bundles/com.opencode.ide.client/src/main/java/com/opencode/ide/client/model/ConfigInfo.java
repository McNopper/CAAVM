package com.opencode.ide.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Minimal mapping of {@code GET /config} - only the fields the IDE needs.
 * {@code model} is the default model in {@code provider/model} format.
 */
public record ConfigInfo(
        String model,
        @SerializedName("small_model") String smallModel) {

    /** @return the default model split into {@code [provider, model]}, or {@code null}. */
    public String[] defaultModelParts() {
        if (model == null) {
            return null;
        }
        int slash = model.indexOf('/');
        if (slash <= 0 || slash == model.length() - 1) {
            return null;
        }
        return new String[] { model.substring(0, slash), model.substring(slash + 1) };
    }
}
