package com.opencode.ide.client.model;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * An agent definition as returned by {@code GET /agent}.
 *
 * <p>Modelled against opencode v1.18.x (the installed server). In this version
 * the agent exposes {@code native} (not {@code builtIn}) and {@code permission}
 * is a flat array of {@code {permission, pattern, action}} rules. The trailing
 * optional fields ({@code model}, {@code temperature}, ...) are not returned by
 * v1.18.x for built-in agents but are kept (nullable) for forward-compatibility
 * with newer opencode releases / custom agents.</p>
 */
public record Agent(
        String name,
        String description,
        String mode,
        @SerializedName("native") Boolean nativeAgent,
        List<PermissionRule> permission,
        Map<String, Object> options,
        Double topP,
        Double temperature,
        String color,
        ModelRef model,
        String prompt,
        Map<String, Boolean> tools,
        Integer maxSteps) {

    public static final String MODE_PRIMARY = "primary";
    public static final String MODE_SUBAGENT = "subagent";
    public static final String MODE_ALL = "all";

    /** @return true for a built-in (non-user-defined) agent. */
    public boolean isNative() {
        return nativeAgent() != null && nativeAgent().booleanValue();
    }

    /** Convenience: true when this agent is user-facing (primary or all). */
    public boolean isPrimary() {
        return MODE_PRIMARY.equals(mode()) || MODE_ALL.equals(mode());
    }

    /**
     * One permission rule. {@code permission} is the tool/category (e.g.
     * {@code "edit"}, {@code "bash"}, {@code "external_directory"}, {@code "*"}),
     * {@code pattern} is a glob, {@code action} is {@code allow}/{@code ask}/{@code deny}.
     */
    public record PermissionRule(String permission, String pattern, String action) {
    }

    /**
     * A model reference. {@code variant} is opencode's reasoning-effort selector
     * (e.g. {@code high}, {@code thinking}); {@code null} = the model default.
     */
    public record ModelRef(String modelID, String providerID, String variant) {
    }
}
