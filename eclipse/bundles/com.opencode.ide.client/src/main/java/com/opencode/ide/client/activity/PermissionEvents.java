package com.opencode.ide.client.activity;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.opencode.ide.client.model.OpencodeEvent;

/**
 * Parses permission-related {@code /event} SSE events into
 * {@link PermissionRequest}s. Shape-tolerant: unknown types yield
 * {@code null}, missing or non-string fields are read leniently, and parsing
 * never throws — a malformed event simply produces {@code null}.
 *
 * <p><b>Event contract (verified against the opencode
 * {@code v1.18.0} tag, {@code packages/schema/src/v1/permission.ts} and the
 * SSE handler in {@code .../httpapi/handlers/event.ts}):</b></p>
 * <ul>
 *   <li>{@code permission.asked} — properties
 *   {@code { id, sessionID, permission, patterns: string[], metadata,
 *   always: string[], tool? }}: a new request is pending. {@code id} is the
 *   {@code per_...} permission id used in the answer endpoint;
 *   {@code permission} the category (e.g. {@code "bash"}); {@code metadata}
 *   an open record (its string values carry display hints like the command).</li>
 *   <li>{@code permission.replied} — properties
 *   {@code { sessionID, requestID, reply: once|always|reject }}: the request
 *   was answered (possibly by another client — e.g. an attached TUI).</li>
 * </ul>
 *
 * <p>Older/other builds reportedly emit a {@code permission.updated}-style
 * event instead; since that shape could not be verified against the server
 * this harness targets, it is intentionally not parsed here. Adding it later
 * only means extending {@link #parse} — the queue
 * ({@code com.opencode.ide.fleet.PermissionQueue}) is keyed by permission id
 * and tolerant of any field gaps.</p>
 */
public final class PermissionEvents {

    /** A request was raised (see class javadoc for the payload shape). */
    public static final String ASKED = "permission.asked";

    /** A request was answered — by us or any other client (see class javadoc). */
    public static final String REPLIED = "permission.replied";

    /** Metadata keys probed (in order) for a display title; string values only. */
    private static final List<String> TITLE_KEYS = List.of("title", "command", "path", "pattern", "description");

    private PermissionEvents() {
    }

    /**
     * Parses one event.
     *
     * @return the {@link PermissionRequest}, or {@code null} for non-permission
     *         events, {@code null}/{@code unknown} types, and permission
     *         events whose session or permission id is missing (nothing
     *         actionable)
     */
    public static PermissionRequest parse(OpencodeEvent event) {
        if (event == null || event.type() == null) {
            return null;
        }
        return switch (event.type()) {
            case ASKED -> parseAsked(event);
            case REPLIED -> parseReplied(event);
            default -> null;
        };
    }

    private static PermissionRequest parseAsked(OpencodeEvent event) {
        String sessionId = first(event.string("sessionID"), event.string("sessionId"));
        String permissionId = first(event.string("id"), event.string("permissionID"),
                event.string("permissionId"));
        if (sessionId == null || permissionId == null) {
            return null;
        }
        return new PermissionRequest(sessionId, permissionId, event.string("permission"),
                strings(event, "patterns"), metadataTitle(event), PermissionRequest.Status.PENDING);
    }

    private static PermissionRequest parseReplied(OpencodeEvent event) {
        String sessionId = first(event.string("sessionID"), event.string("sessionId"));
        String permissionId = first(event.string("requestID"), event.string("permissionID"),
                event.string("id"));
        if (sessionId == null || permissionId == null) {
            return null;
        }
        return new PermissionRequest(sessionId, permissionId, null, List.of(), null,
                PermissionRequest.Status.ANSWERED);
    }

    /** The named property as a string list; non-string entries are skipped. */
    private static List<String> strings(OpencodeEvent event, String key) {
        JsonObject properties = event.properties();
        if (properties == null || !properties.has(key)) {
            return List.of();
        }
        JsonElement element = properties.get(key);
        if (!element.isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                values.add(item.getAsString());
            }
        }
        return List.copyOf(values);
    }

    /** First string metadata value among {@link #TITLE_KEYS}; {@code null} when absent. */
    private static String metadataTitle(OpencodeEvent event) {
        JsonObject properties = event.properties();
        if (properties == null || !properties.has("metadata")) {
            return null;
        }
        JsonElement metadata = properties.get("metadata");
        if (!metadata.isJsonObject()) {
            return null;
        }
        JsonObject object = metadata.getAsJsonObject();
        for (String key : TITLE_KEYS) {
            JsonElement value = object.get(key);
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                String text = value.getAsString();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
