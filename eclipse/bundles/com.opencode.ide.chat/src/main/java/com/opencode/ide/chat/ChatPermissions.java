package com.opencode.ide.chat;

import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.activity.PermissionRequest;

/**
 * Global registry for the chat {@link ChatPermissionSink}: the chat bundle
 * dispatches parsed permission events here; a host bundle (e.g. a permission
 * queue) registers itself with {@link #setSink(ChatPermissionSink)} and
 * unregisters with {@link #clearSink()}. At most one sink — the last set
 * wins, setting {@code null} clears. Thread-safe: registration and dispatch
 * may run on different threads.
 */
public final class ChatPermissions {

    private static volatile ChatPermissionSink sink;

    private ChatPermissions() {
    }

    /** Registers {@code newSink} (replaces any previous one); {@code null} clears. */
    public static void setSink(ChatPermissionSink newSink) {
        sink = newSink;
    }

    /** Removes the registered sink; permission events are ignored again. */
    public static void clearSink() {
        sink = null;
    }

    /**
     * Routes one parsed permission event to the registered sink: a pending
     * ask to {@link ChatPermissionSink#asked} (skipped without a client to
     * answer with), an answer to {@link ChatPermissionSink#replied}. No
     * sink — no-op. A throwing sink is contained so the SSE event loop
     * never breaks.
     */
    static void dispatch(PermissionRequest request, OpencodeClient client) {
        ChatPermissionSink current = sink;
        if (request == null || current == null) {
            return;
        }
        try {
            if (request.pending()) {
                if (client != null) {
                    current.asked(request, client);
                }
            } else {
                current.replied(request.sessionId(), request.permissionId());
            }
        } catch (Throwable ignored) {
            // a broken sink must not break the SSE event loop
        }
    }
}
