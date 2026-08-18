package com.opencode.ide.client.model;

/**
 * Response of {@code GET /global/health}.
 */
public record HealthStatus(boolean healthy, String version) {
}
