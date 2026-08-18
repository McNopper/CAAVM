package com.opencode.ide.chat.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.opencode.ide.client.ChatRequest;
import com.opencode.ide.client.DefaultModels;
import com.opencode.ide.client.OpencodeEventListener;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.OpencodeEvent;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.client.model.Session;

/**
 * Per-view chat session logic (SWT-free): creates and resumes sessions, sends
 * messages through the opencode client, turns {@code message.part.delta}
 * events for the current session into live bubble updates, and reports
 * everything through the {@link Renderer} (the browser page) and {@link Host}
 * (the owning view) callbacks.
 */
public final class ChatSessionController {

    /** Rendering surface driven by the controller (implemented by {@link ChatPage}). */
    public interface Renderer {

        void appendUser(String text);

        void startAssistant(String messageId);

        void appendDelta(String messageId, String text);

        void setAssistantText(String messageId, String text, String reasoning, String meta);

        void setMessages(List<Map<String, String>> rows);

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
                List<Map<String, String>> rows = new ArrayList<>();
                for (ChatEntry entry : entries) {
                    String meta = (entry.info() != null) ? entry.info().modelLabel() : "";
                    rows.add(Map.of(
                            "role", entry.isUser() ? "user" : "assistant",
                            "id", entry.info() != null && entry.info().id() != null ? entry.info().id() : "",
                            "text", entry.text(),
                            "reasoning", entry.reasoning(),
                            "meta", meta));
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
            String mid = (reply != null && reply.info() != null) ? reply.info().id() : "assistant";
            String finalText = (reply != null) ? reply.text() : "";
            String reasoning = (reply != null) ? reply.reasoning() : "";
            String meta = metaFor(reply);
            host.runOnUi(() ->
                    renderer.setAssistantText(mid, finalText, reasoning, meta));
        } catch (OpencodeException e) {
            String failure = e.getMessage();
            host.runOnUi(() -> renderer.notice("⚠ Send failed: " + failure));
        } finally {
            host.runOnUi(() -> {
                sending = false;
                host.sendingChanged(false);
            });
        }
    }

    private static String metaFor(ChatEntry reply) {
        if (reply == null || reply.info() == null) {
            return "";
        }
        return reply.info().modelLabel();
    }
}
