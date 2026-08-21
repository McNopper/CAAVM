package com.opencode.ide.client.model;

/**
 * A session (a running agent instance) as returned by {@code GET /session}.
 *
 * <p>Modelled against opencode v1.18.x. {@code parentID} is present on child
 * sessions (e.g. a subagent spawned by another agent) and is used to nest the
 * session tree. Other fields (slug, title, cost, tokens, time) describe the run.</p>
 */
public record Session(
        String id,
        String slug,
        String title,
        String agent,
        String parentID,
        Time time,
        Double cost,
        Tokens tokens) {

    /** Epoch millis. */
    public record Time(long created, long updated) {
    }

    /** Token usage of the run; the cache breakdown is nullable on some providers. */
    public record Tokens(long input, long output, long reasoning, Cache cache) {
    }

    /** Cache hit (read) vs. fill (write) token counts. */
    public record Cache(long read, long write) {
    }
}
