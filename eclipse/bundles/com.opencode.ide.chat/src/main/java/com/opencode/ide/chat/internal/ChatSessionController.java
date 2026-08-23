package com.opencode.ide.chat.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.opencode.ide.client.ChatRequest;
import com.opencode.ide.client.DefaultModels;
import com.opencode.ide.client.OpencodeEventListener;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.ChatPart;
import com.opencode.ide.client.model.OpencodeEvent;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.client.model.Session;

/**
 * Per-view chat session logic (SWT-free): creates and resumes sessions, sends
 * messages through the opencode client, turns {@code message.part.delta}
 * events for the current session into live bubble updates, aborts in-flight
 * replies ({@link #abort()}), and reports everything through the
 * {@link Renderer} (the browser page) and {@link Host} (the owning view)
 * callbacks.
 */
public final class ChatSessionController {

    /**
     * One tool-call line for compact rendering: the tool name plus a coarse
     * state ({@code running}/{@code completed}/{@code error}) extracted from
     * the {@code tool} parts of a {@link ChatEntry}.
     */
    public record ToolLine(String name, String state) {
    }

    /** Rendering surface driven by the controller (implemented by {@link ChatPage}). */
    public interface Renderer {

        void appendUser(String text);

        void startAssistant(String messageId);

        void appendDelta(String messageId, String text);

        void setAssistantText(String messageId, String text, String reasoning, String meta,
                List<ToolLine> tools);

        /** Stops the streaming cursor of a bubble (send completed/failed or aborted). */
        void stopStream(String messageId);

        void setMessages(List<Map<String, Object>> rows);

        void notice(String text);

        void clear();
    }

    /** Ambient services the controller needs from its host view. */
    public interface Host {

        /** Runs {@code task} in a named background job (never on the UI thread). */
        void runInBackground(String jobName, Runnable task);

        /** Dispatches {@code task} to the UI thread. */
        void runOnUi(Runnable task);

        /** Info-level bundle log. */
        void info(String message);

        /** Error-level bundle log. */
        void error(String message, Throwable throwable);

        /** View content description changed (e.g. current session id). */
        void statusChanged(String description);

        /** A message is in flight ({@code true}) or done ({@code false}) - send button state. */
        void sendingChanged(boolean sending);
    }

    /** One prompt to send: the typed text plus the current agent/model/variant pick. */
    public record OutgoingMessage(
            String agent,
            String providerId,
            String modelId,
            String variant,
            String system,
            String text) {
    }

    /** Receives the agents/models fetched for the selector combos. */
    public interface SelectorDataListener {

        void loaded(List<Agent> agents, ProviderList providers, String[] defaultModel);

        void failed(OpencodeException error);
    }

    private final ChatServerConnection connection;
    private final Renderer renderer;
    private final Host host;

    private volatile String sessionId;
    private volatile boolean sending;
    private volatile String[] defaultModelParts;
    /**
     * Every mid that streamed a bubble during the current send, in arrival
     * order (a tool round produces several assistant messages). The last one
     * is the final-render target; all of them must get a cursor stop.
     */
    private final List<String> streamedMids = new CopyOnWriteArrayList<>();
    private OpencodeEventListener eventListener;

    public ChatSessionController(ChatServerConnection connection, Renderer renderer, Host host) {
        this.connection = connection;
        this.renderer = renderer;
        this.host = host;
    }

    // ---------- lifecycle ----------

    /** Subscribes to the opencode event stream (deltas for this session). */
    public void subscribe() {
        eventListener = event -> {
            if (event == null || event.type() == null) {
                return;
            }
            String sid = sessionId;
            if (sid == null) {
                return;
            }
            if ("message.part.delta".equals(event.type())) {
                String partSession = event.string("sessionID");
                String messageId = event.string("messageID");
                String field = event.string("field");
                String delta = event.string("delta");
                if (!sid.equals(partSession) || messageId == null || delta == null || delta.isEmpty()) {
                    return;
                }
                if (!"text".equals(field)) {
                    return;
                }
                // A delta for an unknown mid while nothing is in flight is a
                // late event after the send settled (or another client's
                // message): rendering it would orphan a bubble whose cursor
                // nobody ever stops. Continuations of known bubbles are fine.
                boolean known = streamedMids.contains(messageId);
                if (!sending && !known) {
                    return;
                }
                if (!known) {
                    streamedMids.add(messageId);
                }
                host.runOnUi(() -> {
                    renderer.startAssistant(messageId);
                    renderer.appendDelta(messageId, delta);
                });
            }
        };
        connection.addEventListener(eventListener);
    }

    /** Unsubscribes from the event stream (view dispose). */
    public void dispose() {
        if (eventListener != null) {
            try {
                connection.removeEventListener(eventListener);
            } catch (Throwable ignored) {
                // best-effort during dispose
            }
            eventListener = null;
        }
    }

    /** @return true while a message is in flight (guards double sends). */
    public boolean isSending() {
        return sending;
    }

    /** @return the current session id, or {@code null} before the first message. */
    public String sessionId() {
        return sessionId;
    }

    /** Remembers the default model used when the user has not picked one. */
    public void setDefaultModel(String providerId, String modelId) {
        defaultModelParts = (providerId == null || modelId == null)
                ? null
                : new String[] { providerId, modelId };
    }

    // ---------- sessions ----------

    /** Drops the current session; the next message starts a fresh one. */
    public void startNewSession() {
        sessionId = null;
        renderer.clear();
        host.statusChanged("New session (created on first message)");
        renderer.notice("Fresh session - your next message starts a new conversation.");
    }

    /** Resumes {@code sid}: loads its history into the transcript. */
    public void resume(String sid) {
        sessionId = sid;
        host.runInBackground("Loading chat history " + sid, () -> {
            try {
                List<ChatEntry> entries = connection.getClient().getMessages(sid);
                List<Map<String, Object>> rows = new ArrayList<>();
                for (ChatEntry entry : entries) {
                    String meta = (entry.info() != null) ? entry.info().modelLabel() : "";
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("role", entry.isUser() ? "user" : "assistant");
                    row.put("id", entry.info() != null && entry.info().id() != null ? entry.info().id() : "");
                    row.put("text", entry.text());
                    row.put("reasoning", entry.reasoning());
                    row.put("meta", meta);
                    row.put("tools", toolLinesOf(entry));
                    rows.add(row);
                }
                host.runOnUi(() -> {
                    renderer.setMessages(rows);
                    renderer.notice("Resumed session " + sid + " - continuing the conversation.");
                });
            } catch (OpencodeException e) {
                host.runOnUi(() -> host.statusChanged("Error loading history: " + e.getMessage()));
            }
        });
    }

    /** Compact tool-call lines of an entry ({@code tool} parts: name + state). */
    private static List<ToolLine> toolLinesOf(ChatEntry entry) {
        if (entry == null) {
            return List.of();
        }
        List<ToolLine> tools = new ArrayList<>();
        for (ChatPart part : entry.parts()) {
            if (part.isTool()) {
                tools.add(new ToolLine(
                        part.tool() != null ? part.tool() : "",
                        part.stateName() != null ? part.stateName() : ""));
            }
        }
        return tools;
    }

    // ---------- selectors ----------

    /** Fetches agents/providers plus the default model for the selector combos. */
    public void loadSelectorData(SelectorDataListener listener) {
        host.runInBackground("Loading opencode agents and models", () -> {
            try {
                List<Agent> agents = connection.getClient().getAgents();
                ProviderList providers = connection.getClient().getProviders();
                String[] fallback = DefaultModels.resolve(connection.getClient().getConfig(), providers);
                host.runOnUi(() -> listener.loaded(agents, providers, fallback));
            } catch (OpencodeException e) {
                host.runOnUi(() -> listener.failed(e));
            }
        });
    }

    // ---------- sending ----------

    /** Sends one prompt; renders the echo immediately and the final reply on completion. */
    public void send(OutgoingMessage message) {
        if (sending) {
            return;
        }
        sending = true;
        streamedMids.clear();
        try {
            host.sendingChanged(true);
            host.info("send: begin (" + message.text().length() + " chars)");
            renderer.appendUser(message.text());
            host.runInBackground("Sending opencode chat message", () -> runSendJob(message));
        } catch (Throwable t) {
            host.error("send failed unexpectedly", t);
            sending = false;
            host.sendingChanged(false);
        }
    }

    private void runSendJob(OutgoingMessage message) {
        try {
            host.info("send job: running");
            String sid = sessionId;
            if (sid == null) {
                Session session = connection.getClient().createSession("Eclipse Chat");
                sid = session.id();
                sessionId = sid;
                String finalSid = sid;
                host.runOnUi(() -> host.statusChanged("Session " + finalSid));
            }

            String providerId = message.providerId();
            String modelId = message.modelId();
            if (providerId == null || modelId == null) {
                String[] fallback = defaultModelParts;
                if (fallback == null) {
                    fallback = DefaultModels.resolve(
                            connection.getClient().getConfig(),
                            connection.getClient().getProviders());
                }
                if (fallback == null) {
                    host.runOnUi(() -> renderer.notice("⚠ No model available on the server."));
                    return;
                }
                providerId = fallback[0];
                modelId = fallback[1];
            }

            ChatRequest request = new ChatRequest(sid, message.agent(), providerId, modelId,
                    message.variant(), message.system(), message.text());
            ChatEntry reply = connection.getClient().sendMessage(request);
            // The final render MUST target a bubble the deltas streamed into — the POST
            // response id and the SSE messageID can differ across server versions, and a
            // mismatched id would orphan the streaming bubble with its blinking cursor.
            // With tool rounds there are several streamed bubbles: the reply is the LAST.
            String lastStreamed = streamedMids.isEmpty() ? null
                    : streamedMids.get(streamedMids.size() - 1);
            boolean streamedContent = !streamedMids.isEmpty();
            boolean replyEmpty = reply == null || reply.text() == null || reply.text().isEmpty();
            if (streamedContent && replyEmpty) {
                // The server returned no authoritative text (observed when the
                // run used tools): keep the streamed bubbles — the cursor stop
                // finalizes their raw text into rendered markdown on the page.
                host.info("send job: empty reply, keeping " + streamedMids.size() + " streamed bubble(s)");
            } else {
                String mid = lastStreamed != null ? lastStreamed
                        : ((reply != null && reply.info() != null) ? reply.info().id() : "assistant");
                String finalText = (reply != null) ? reply.text() : "";
                String reasoning = (reply != null) ? reply.reasoning() : "";
                String meta = metaFor(reply);
                List<ToolLine> tools = toolLinesOf(reply);
                host.runOnUi(() ->
                        renderer.setAssistantText(mid, finalText, reasoning, meta, tools));
            }
        } catch (OpencodeException e) {
            String failure = e.getMessage();
            host.runOnUi(() -> renderer.notice("⚠ Send failed: " + failure));
        } finally {
            // Flip the flag first (volatile): any SSE delta arriving after this
            // sees sending == false and must not spawn an orphan bubble.
            sending = false;
            // every streamed bubble gets its cursor stopped (first, middle, last)
            List<String> streamed = List.copyOf(streamedMids);
            streamedMids.clear();
            for (String mid : streamed) {
                // belt and braces: even on failure/abort the cursor must stop
                host.runOnUi(() -> renderer.stopStream(mid));
            }
            host.runOnUi(() -> host.sendingChanged(false));
        }
    }

    // ---------- abort ----------

    /**
     * Aborts the in-flight reply: shows an interrupted notice immediately, then
     * POSTs the abort endpoint on a background thread (never the UI thread).
     * The {@code sending} flag itself is cleared by the aborted send job when
     * the server unblocks its reply call. A no-op when nothing is in flight or
     * the session does not exist yet (first message still creating it).
     */
    public void abort() {
        String sid = sessionId;
        if (!sending || sid == null) {
            return;
        }
        for (String mid : List.copyOf(streamedMids)) {
            renderer.stopStream(mid); // no interrupted bubble may keep blinking
        }
        renderer.notice("⏹ Aborted by user.");
        host.runInBackground("Aborting opencode chat " + sid, () -> {
            try {
                connection.getClient().abortSession(sid);
                host.info("abort: posted for session " + sid);
            } catch (OpencodeException e) {
                host.error("abort failed for session " + sid, e);
                host.runOnUi(() -> renderer.notice("⚠ Abort failed: " + e.getMessage()));
            }
        });
    }

    private static String metaFor(ChatEntry reply) {
        if (reply == null || reply.info() == null) {
            return "";
        }
        return reply.info().modelLabel();
    }
}
