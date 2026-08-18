package com.opencode.ide.client.model;

/**
 * Status of a session from {@code GET /session/status}: a map of
 * {@code sessionId -> SessionStatus}. The {@code type} is
 * {@code "idle"} / {@code "busy"} / {@code "retry"}.
 */
public record SessionStatus(String type) {
}
