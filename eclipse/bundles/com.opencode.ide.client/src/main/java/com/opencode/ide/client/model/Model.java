package com.opencode.ide.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A model as returned nested under a {@link Provider}.
 *
 * Mirrors the {@code Model} type in the opencode OpenAPI schema.
 */
public record Model(
        String id,
        String providerID,
        Api api,
        String name,
        String family,
        Capabilities capabilities,
        Cost cost,
        Limit limit,
        String status,
        Map<String, Object> options,
        Map<String, String> headers,
        Map<String, Object> variants) {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_BETA = "beta";
    public static final String STATUS_ALPHA = "alpha";
    public static final String STATUS_DEPRECATED = "deprecated";

    /**
     * Variant names in server order (e.g. {@code none, low, medium, high, xhigh, max}
     * or {@code none, thinking}), empty when the model has no variants.
     *
     * <p>Variants are opencode's per-request reasoning-effort settings: they are
     * sent as {@code "variant": "<name>"} on {@code POST /session/:id/message}.</p>
     */
    public List<String> variantNames() {
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(variants.keySet()); // Gson keeps JSON order (LinkedTreeMap)
    }

    /** @return true when {@code name} is a variant this model supports. */
    public boolean hasVariant(String name) {
        return name != null && variants != null && variants.containsKey(name);
    }

    /** The model's provider API endpoint (rarely set by local providers). */
    public record Api(String id, String url, String npm) {
    }

    /** What the model accepts/supports; drives the Providers view's R/A/T letters. */
    public record Capabilities(
            boolean temperature,
            boolean reasoning,
            boolean attachment,
            boolean toolcall,
            Modalities input,
            Modalities output) {
    }

    /** Input/output modality switches. */
    public record Modalities(boolean text, boolean audio, boolean image, boolean video, boolean pdf) {
    }

    /** Per-million-token prices (USD) — the basis of cost telemetry. */
    public record Cost(double input, double output, CacheCost cache, ExperimentalCost experimentalOver200K) {
    }

    /** Cached-token prices (cheaper than fresh input). */
    public record CacheCost(double read, double write) {
    }

    /** Prices above the 200K-token threshold some providers use. */
    public record ExperimentalCost(double input, double output, CacheCost cache) {
    }

    /** Context/output token limits — the context column in the Providers view. */
    public record Limit(long context, long output) {
    }
}
