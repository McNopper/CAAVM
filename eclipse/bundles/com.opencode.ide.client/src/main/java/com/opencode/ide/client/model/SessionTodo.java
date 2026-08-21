package com.opencode.ide.client.model;

/**
 * One entry of a session's todo list from {@code GET /session/:id/todo}
 * (opencode v1.18): a flat {@code {id, content, status, priority}} object.
 *
 * <p>Nullable-tolerant like the other DTOs: the server may omit any field
 * (Gson maps missing fields to {@code null}); a {@code null} {@code status}
 * means "not done" for callers, and entries with a {@code null} content are
 * skipped. Note that ids belong to the opencode server's todo list - they are
 * NOT the task store's todo identity (text reuse is, see the fleet telemetry).
 * </p>
 */
public record SessionTodo(String id, String content, String status, String priority) {
}
