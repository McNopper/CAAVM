package com.opencode.ide.client.model;

/**
 * One auth method of a provider ({@code GET /provider/auth}). The live v1.18
 * wire shape is a <b>map</b> keyed by provider id whose values are lists of
 * {@code {"type":"oauth"|"api","label":"…"}} methods - the client converts it
 * into this flat, one-entry-per-method form (same conversion as
 * {@link McpServerInfo} for {@code GET /mcp}). Nullable-tolerant.
 */
public record ProviderAuth(
        String provider,
        String type,
        String label) {
}
