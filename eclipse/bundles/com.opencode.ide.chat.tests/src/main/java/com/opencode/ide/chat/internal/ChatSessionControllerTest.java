package com.opencode.ide.chat.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.opencode.ide.client.ChatRequest;
import com.opencode.ide.client.McpServerConfig;
import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.OpencodeEventListener;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.ChatMessageInfo;
import com.opencode.ide.client.model.ChatPart;
import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.Model;
import com.opencode.ide.client.model.OpencodeEvent;
import com.opencode.ide.client.model.Provider;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SessionStatus;

/**
 * Unit tests for the session logic extracted from the (formerly ~700-line)
 * ChatView: sending (session creation, model fallback, final render, failure
 * notices), resume, live deltas, and disposal - against fake
 * connection/renderer/host collaborators, with inline executors.
 */
public class ChatSessionControllerTest {

    private FakeConnection connection;
    private RecordingRenderer renderer;
    private FakeHost host;
    private ChatSessionController controller;

    @Before
    public void setUp() {
        connection = new FakeConnection();
        renderer = new RecordingRenderer();
        host = new FakeHost();
        controller = new ChatSessionController(connection, renderer, host);
    }

    // ---------- sending ----------

    @Test
    public void sendCreatesSessionEchoesPromptAndRendersFinalReply() {
        controller.send(new ChatSessionController.OutgoingMessage(
                "build", "prov", "m1", "high", "sys", "hello"));

        assertEquals(List.of("hello"), renderer.users);
        assertEquals(1, connection.client.requests.size());
        assertEquals(new ChatRequest("ses_1", "build", "prov", "m1", "high", "sys", "hello"),
                connection.client.requests.get(0));
        assertTrue("final render expected, got: " + renderer.assistants,
                renderer.assistants.contains("final:msg_1:done||prov/mod|"));
        assertEquals(List.of("Session ses_1"), host.statuses);
        assertFalse(controller.isSending());
        assertEquals(Boolean.FALSE, host.sendingStates.get(host.sendingStates.size() - 1));
        // explicit model pick: no default-model lookups against the server
        assertEquals(0, connection.client.configCalls);
        assertEquals(0, connection.client.providersCalls);
        // the original log markers
        assertTrue(host.infos.contains("send: begin (5 chars)"));
        assertTrue(host.infos.contains("send job: running"));
    }

    @Test
    public void sendFinalRenderCarriesToolParts() {
        connection.client.reply = new ChatEntry(
                new ChatMessageInfo("msg_t", "ses_1", "assistant", null, null, null, null,
                        null, null, "prov", "mod", null, null),
                List.of(new ChatPart("text", "done", null, null),
                        new ChatPart("tool", null, "read", new ChatPart.ToolState("completed")),
                        new ChatPart("tool", null, "cmake_build", new ChatPart.ToolState("error")),
                        new ChatPart("step-start", null, null, null)));
        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "hi"));

        assertTrue("tool lines expected in the final render, got: " + renderer.assistants,
                renderer.assistants.contains("final:msg_t:done||prov/mod|read/completed,cmake_build/error"));
    }

    @Test
    public void sendWithoutExplicitModelUsesRememberedDefault() {
        controller.setDefaultModel("prov", "m1");
        controller.send(new ChatSessionController.OutgoingMessage(
                null, null, null, null, null, "hi"));

        assertEquals(1, connection.client.requests.size());
        assertEquals("prov", connection.client.requests.get(0).providerId());
        assertEquals("m1", connection.client.requests.get(0).modelId());
        assertEquals(0, connection.client.configCalls);
        assertEquals(0, connection.client.providersCalls);
    }

    @Test
    public void sendWithoutAnyModelShowsNoticeAndSendsNothing() {
        // no remembered default; client reports no providers -> nothing resolvable
        controller.send(new ChatSessionController.OutgoingMessage(
                null, null, null, null, null, "hi"));

        assertEquals(List.of("⚠ No model available on the server."), renderer.notices);
        assertTrue(connection.client.requests.isEmpty());
        assertFalse(controller.isSending());
        assertEquals(Boolean.TRUE, host.sendingStates.get(0));
        assertEquals(Boolean.FALSE, host.sendingStates.get(host.sendingStates.size() - 1));
    }

    @Test
    public void sendFailureShowsNoticeAndStopsSending() {
        connection.client.sendFailure = new OpencodeException("boom");
        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "hi"));

        assertEquals(List.of("⚠ Send failed: boom"), renderer.notices);
        assertTrue(renderer.assistants.isEmpty());
        assertFalse(controller.isSending());
    }

    @Test
    public void secondSendIsIgnoredWhileOneIsInFlight() {
        host.holdBackground = true;
        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "first"));
        assertTrue(controller.isSending());

        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "second"));
        assertEquals(List.of("first"), renderer.users);

        host.holdBackground = false;
        host.queuedBackground.forEach(Runnable::run);
        assertFalse(controller.isSending());
        assertEquals(1, connection.client.requests.size());
        assertEquals("first", connection.client.requests.get(0).text());
    }

    // ---------- abort ----------

    @Test
    public void abortWhileSendingPostsAbortForTheSessionAndNotifies() {
        controller.resume("ses_9");
        host.holdBackground = true;
        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "hi"));
        assertTrue(controller.isSending());

        controller.abort();
        assertTrue("interrupted notice expected, got: " + renderer.notices,
                renderer.notices.contains("⏹ Aborted by user."));
        assertTrue(host.jobs.contains("Aborting opencode chat ses_9"));
        // the abort POST runs on the background job, never the calling thread
        assertTrue(connection.client.abortCalls.isEmpty());

        host.holdBackground = false;
        host.queuedBackground.forEach(Runnable::run);
        assertEquals(List.of("ses_9"), connection.client.abortCalls);
        // the aborted send job still completes (the server unblocks its reply
        // call) and re-enables the send button
        assertFalse(controller.isSending());
        assertEquals(Boolean.FALSE, host.sendingStates.get(host.sendingStates.size() - 1));
    }

    @Test
    public void abortWithoutSendOrSessionIsANoOp() {
        controller.abort();
        assertTrue(renderer.notices.isEmpty());
        assertTrue(connection.client.abortCalls.isEmpty());

        // sending, but the session is not created yet (first send job queued)
        host.holdBackground = true;
        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "hi"));
        controller.abort();
        assertTrue("nothing to abort yet, got: " + renderer.notices,
                renderer.notices.isEmpty());

        host.queuedBackground.forEach(Runnable::run);
        assertTrue(connection.client.abortCalls.isEmpty());
        assertFalse(controller.isSending());
    }

    @Test
    public void abortFailureIsLoggedAndShownAsNotice() {
        connection.client.abortFailure = new OpencodeException("nope");
        controller.resume("ses_9");
        host.holdBackground = true;
        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "hi"));
        controller.abort();
        host.holdBackground = false;
        host.queuedBackground.forEach(Runnable::run);

        assertEquals(1, connection.client.abortCalls.size());
        assertTrue(renderer.notices.contains("⚠ Abort failed: nope"));
        assertTrue(host.infos.contains("ERROR abort failed for session ses_9"));
    }

    // ---------- live deltas ----------

    @Test
    public void textDeltasForThisSessionRenderLive() {
        controller.subscribe();
        assertEquals(1, connection.listeners.size());

        // before any session exists, deltas are ignored
        connection.fire(deltaEvent("{\"sessionID\":\"ses_1\",\"messageID\":\"m\",\"delta\":\"x\"}"));
        assertTrue(renderer.deltas.isEmpty());

        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "hi"));
        connection.fire(deltaEvent(
                "{\"sessionID\":\"ses_1\",\"messageID\":\"msg_1\",\"field\":\"text\",\"delta\":\"chunk\"}"));
        assertEquals(List.of("start:msg_1"), renderer.assistants.stream()
                .filter(a -> a.startsWith("start:")).toList());
        assertEquals(List.of("msg_1:chunk"), renderer.deltas);

        // other session / empty delta / non-text field / missing id: all ignored
        connection.fire(deltaEvent(
                "{\"sessionID\":\"ses_other\",\"messageID\":\"msg_1\",\"field\":\"text\",\"delta\":\"x\"}"));
        connection.fire(deltaEvent(
                "{\"sessionID\":\"ses_1\",\"messageID\":\"msg_1\",\"field\":\"text\",\"delta\":\"\"}"));
        connection.fire(deltaEvent(
                "{\"sessionID\":\"ses_1\",\"messageID\":\"msg_1\",\"field\":\"reasoning\",\"delta\":\"x\"}"));
        connection.fire(deltaEvent("{\"sessionID\":\"ses_1\",\"field\":\"text\",\"delta\":\"x\"}"));
        assertEquals(1, renderer.deltas.size());

        controller.dispose();
        assertTrue(connection.listeners.isEmpty());
    }

    // ---------- resume / new session ----------

    @Test
    public void resumeRendersHistoryAndNotice() {
        connection.client.history = List.of(
                entry("u1", "user", "question"),
                entry("a1", "assistant", "answer"));
        controller.resume("ses_42");

        assertEquals("ses_42", controller.sessionId());
        assertEquals(1, renderer.histories.size());
        List<Map<String, Object>> rows = renderer.histories.get(0);
        assertEquals(2, rows.size());
        assertEquals("user", rows.get(0).get("role"));
        assertEquals("question", rows.get(0).get("text"));
        assertEquals("a1", rows.get(1).get("id"));
        assertEquals("prov/mod", rows.get(1).get("meta"));
        assertEquals(List.of("Resumed session ses_42 - continuing the conversation."),
                renderer.notices);
        assertTrue(host.jobs.contains("Loading chat history ses_42"));
    }

    @Test
    public void resumePassesToolPartsToTheRenderer() {
        connection.client.history = List.of(
                entry("u1", "user", "question"),
                new ChatEntry(
                        new ChatMessageInfo("a1", "ses_1", "assistant", null, null, null, null,
                                null, null, "prov", "mod", null, null),
                        List.of(new ChatPart("text", "answer", null, null),
                                new ChatPart("tool", null, "read", new ChatPart.ToolState("completed")),
                                new ChatPart("tool", null, "cmake_build", new ChatPart.ToolState("error")))));
        controller.resume("ses_42");

        List<Map<String, Object>> rows = renderer.histories.get(0);
        assertEquals(List.of(), rows.get(0).get("tools")); // user rows carry an empty list
        @SuppressWarnings("unchecked")
        List<ChatSessionController.ToolLine> tools =
                (List<ChatSessionController.ToolLine>) rows.get(1).get("tools");
        assertEquals(2, tools.size());
        assertEquals("read", tools.get(0).name());
        assertEquals("completed", tools.get(0).state());
        assertEquals("cmake_build", tools.get(1).name());
        assertEquals("error", tools.get(1).state());
    }

    @Test
    public void resumeFailureUpdatesStatus() {
        connection.client.historyFailure = new OpencodeException("gone");
        controller.resume("ses_42");

        assertEquals(List.of("Error loading history: gone"), host.statuses);
        assertTrue(renderer.histories.isEmpty());
    }

    @Test
    public void newSessionClearsTranscriptAndUsesAFreshSessionOnTheNextSend() {
        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "one"));
        assertEquals("ses_1", controller.sessionId());

        controller.startNewSession();
        assertNull(controller.sessionId());
        assertEquals(1, renderer.clears);
        assertTrue(host.statuses.contains("New session (created on first message)"));
        assertTrue(renderer.notices.contains(
                "Fresh session - your next message starts a new conversation."));

        controller.send(new ChatSessionController.OutgoingMessage(
                null, "prov", "m1", null, null, "two"));
        assertEquals("ses_2", controller.sessionId());
        assertEquals(2, connection.client.createdSessions.size());
    }

    // ---------- selectors ----------

    @Test
    public void selectorDataDeliversAgentsProvidersAndResolvedDefault() {
        Agent agent = new Agent("build", "d", "primary", Boolean.TRUE,
                null, null, null, null, null, null, null, null, null);
        Model model = new Model("m1", "prov", null, null, null, null, null, null,
                null, null, null, null);
        Provider provider = new Provider("prov", "Prov", null, null, null, null,
                Map.of("m1", model));
        connection.client.agents = List.of(agent);
        connection.client.providers = new ProviderList(List.of(provider), Map.of());

        final List<Agent> gotAgents = new ArrayList<>();
        final List<String[]> gotDefault = new ArrayList<>();
        controller.loadSelectorData(new ChatSessionController.SelectorDataListener() {
            @Override
            public void loaded(List<Agent> agents, ProviderList providers, String[] defaultModel) {
                gotAgents.addAll(agents);
                gotDefault.add(defaultModel);
            }

            @Override
            public void failed(OpencodeException error) {
                throw new AssertionError(error);
            }
        });

        assertEquals(1, gotAgents.size());
        assertSame(agent, gotAgents.get(0));
        assertEquals(1, gotDefault.size());
        assertEquals("prov", gotDefault.get(0)[0]);
        assertEquals("m1", gotDefault.get(0)[1]);
        assertTrue(host.jobs.contains("Loading opencode agents and models"));
    }

    @Test
    public void selectorDataFailureIsForwarded() {
        connection.client.agentsFailure = new OpencodeException("down");
        final List<OpencodeException> failures = new ArrayList<>();
        controller.loadSelectorData(new ChatSessionController.SelectorDataListener() {
            @Override
            public void loaded(List<Agent> agents, ProviderList providers, String[] defaultModel) {
                throw new AssertionError("expected failure");
            }

            @Override
            public void failed(OpencodeException error) {
                failures.add(error);
            }
        });

        assertEquals(1, failures.size());
        assertEquals("down", failures.get(0).getMessage());
    }

    // ---------- helpers / fakes ----------

    // ---------- streaming cursor lifecycle ----------

    @Test
    public void finalRenderTargetsTheStreamedBubbleEvenWhenIdsDiffer() {
        controller.subscribe();
        controller.send(new ChatSessionController.OutgoingMessage(
                "build", "prov", "m1", null, null, "hello")); // creates ses_1, completes
        host.holdBackground = true;
        controller.send(new ChatSessionController.OutgoingMessage(
                "build", "prov", "m1", null, null, "again"));
        // deltas stream into msg_stream while the POST is still in flight; the
        // reply carries a DIFFERENT id — the final render must hit the streamed
        // bubble or its blinking cursor never stops
        connection.fire(deltaEvent(
                "{\"sessionID\":\"ses_1\",\"messageID\":\"msg_stream\",\"field\":\"text\",\"delta\":\"par\"}"));
        connection.client.reply = entry("msg_reply", "assistant", "done");
        host.queuedBackground.forEach(Runnable::run);

        assertTrue("final render must target the streamed mid, got: " + renderer.assistants,
                renderer.assistants.stream().anyMatch(a -> a.startsWith("final:msg_stream:")));
        assertTrue("stream stop expected after the send settles, got: " + renderer.assistants,
                renderer.assistants.stream().anyMatch(a -> a.equals("stop:msg_stream")));
    }

    @Test
    public void failedSendStopsTheStreamingCursor() {
        controller.subscribe();
        controller.send(new ChatSessionController.OutgoingMessage(
                "build", "prov", "m1", null, null, "hello")); // creates ses_1, completes
        host.holdBackground = true;
        controller.send(new ChatSessionController.OutgoingMessage(
                "build", "prov", "m1", null, null, "again"));
        connection.fire(deltaEvent(
                "{\"sessionID\":\"ses_1\",\"messageID\":\"msg_stream\",\"field\":\"text\",\"delta\":\"par\"}"));
        connection.client.sendFailure = new OpencodeException("boom");
        host.queuedBackground.forEach(Runnable::run);

        assertTrue(renderer.notices.stream().anyMatch(n -> n.startsWith("⚠ Send failed")));
        assertTrue("cursor stop must fire even on failure, got: " + renderer.assistants,
                renderer.assistants.stream().anyMatch(a -> a.equals("stop:msg_stream")));
    }

    @Test
    public void abortStopsTheStreamingCursorImmediately() {
        controller.subscribe();
        controller.send(new ChatSessionController.OutgoingMessage(
                "build", "prov", "m1", null, null, "hello")); // creates ses_1, completes
        host.holdBackground = true;
        controller.send(new ChatSessionController.OutgoingMessage(
                "build", "prov", "m1", null, null, "again"));
        connection.fire(deltaEvent(
                "{\"sessionID\":\"ses_1\",\"messageID\":\"msg_stream\",\"field\":\"text\",\"delta\":\"par\"}"));

        controller.abort();

        assertTrue("abort must stop the cursor, got: " + renderer.assistants,
                renderer.assistants.stream().anyMatch(a -> a.equals("stop:msg_stream")));
        assertTrue(renderer.notices.contains("⏹ Aborted by user."));
        host.queuedBackground.clear(); // never completes; nothing more to assert
    }

    private static OpencodeEvent deltaEvent(String propertiesJson) {
        JsonObject properties = new Gson().fromJson(propertiesJson, JsonObject.class);
        return new OpencodeEvent("message.part.delta", properties);
    }

    private static ChatEntry entry(String id, String role, String text) {
        ChatMessageInfo info = new ChatMessageInfo(id, "ses_1", role, null, null, null,
                null, null, null, "prov", "mod", null, null);
        return new ChatEntry(info, List.of(new ChatPart("text", text, null, null)));
    }

    private static final class RecordingRenderer implements ChatSessionController.Renderer {
        final List<String> users = new ArrayList<>();
        final List<String> assistants = new ArrayList<>();
        final List<String> deltas = new ArrayList<>();
        final List<String> notices = new ArrayList<>();
        final List<List<Map<String, Object>>> histories = new ArrayList<>();
        int clears;

        @Override
        public void appendUser(String text) {
            users.add(text);
        }

        @Override
        public void startAssistant(String messageId) {
            assistants.add("start:" + messageId);
        }

        @Override
        public void appendDelta(String messageId, String text) {
            deltas.add(messageId + ":" + text);
        }

        @Override
        public void setAssistantText(String messageId, String text, String reasoning, String meta,
                List<ChatSessionController.ToolLine> tools) {
            StringBuilder toolText = new StringBuilder();
            for (ChatSessionController.ToolLine tool : tools) {
                if (toolText.length() > 0) {
                    toolText.append(",");
                }
                toolText.append(tool.name()).append("/").append(tool.state());
            }
            assistants.add("final:" + messageId + ":" + text + "|" + reasoning + "|" + meta
                    + "|" + toolText);
        }

        @Override
        public void stopStream(String messageId) {
            assistants.add("stop:" + messageId);
        }

        @Override
        public void setMessages(List<Map<String, Object>> rows) {
            histories.add(rows);
        }

        @Override
        public void notice(String text) {
            notices.add(text);
        }

        @Override
        public void clear() {
            clears++;
        }
    }

    private static final class FakeHost implements ChatSessionController.Host {
        final List<String> infos = new ArrayList<>();
        final List<String> jobs = new ArrayList<>();
        final List<String> statuses = new ArrayList<>();
        final List<Boolean> sendingStates = new ArrayList<>();
        final List<Runnable> queuedBackground = new ArrayList<>();
        boolean holdBackground;

        @Override
        public void runInBackground(String jobName, Runnable task) {
            jobs.add(jobName);
            if (holdBackground) {
                queuedBackground.add(task);
            } else {
                task.run();
            }
        }

        @Override
        public void runOnUi(Runnable task) {
            task.run();
        }

        @Override
        public void info(String message) {
            infos.add(message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            infos.add("ERROR " + message);
        }

        @Override
        public void statusChanged(String description) {
            statuses.add(description);
        }

        @Override
        public void sendingChanged(boolean sending) {
            sendingStates.add(sending);
        }
    }

    private static final class FakeConnection implements ChatServerConnection {
        final FakeClient client = new FakeClient();
        final List<OpencodeEventListener> listeners = new ArrayList<>();

        @Override
        public OpencodeClient getClient() {
            return client;
        }

        @Override
        public void addEventListener(OpencodeEventListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeEventListener(OpencodeEventListener listener) {
            listeners.remove(listener);
        }

        void fire(OpencodeEvent event) {
            for (OpencodeEventListener listener : List.copyOf(listeners)) {
                listener.onEvent(event);
            }
        }
    }

    private static final class FakeClient implements OpencodeClient {
        final List<ChatRequest> requests = new ArrayList<>();
        final List<String> createdSessions = new ArrayList<>();
        final List<String> abortCalls = new ArrayList<>();
        int sessionCounter;
        List<Agent> agents = List.of();
        OpencodeException agentsFailure;
        ProviderList providers = new ProviderList(List.of(), Map.of());
        ChatEntry reply = entry("msg_1", "assistant", "done");
        OpencodeException sendFailure;
        List<ChatEntry> history = List.of();
        OpencodeException historyFailure;
        OpencodeException abortFailure;
        int configCalls;
        int providersCalls;

        @Override
        public HealthStatus getHealth() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Agent> getAgents() throws OpencodeException {
            if (agentsFailure != null) {
                throw agentsFailure;
            }
            return agents;
        }

        @Override
        public ProviderList getProviders() {
            providersCalls++;
            return providers;
        }

        @Override
        public ConfigInfo getConfig() {
            configCalls++;
            return null;
        }

        @Override
        public List<Session> getSessions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, SessionStatus> getSessionStatus() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Session createSession(String title, Path directory) {
            createdSessions.add(title);
            sessionCounter++;
            return new Session("ses_" + sessionCounter, null, title, null, null, null, null, null);
        }

        @Override
        public void registerMcp(String name, McpServerConfig config) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ChatEntry> getMessages(String sessionId) throws OpencodeException {
            if (historyFailure != null) {
                throw historyFailure;
            }
            return history;
        }

        @Override
        public ChatEntry sendMessage(ChatRequest request) throws OpencodeException {
            requests.add(request);
            if (sendFailure != null) {
                throw sendFailure;
            }
            return reply;
        }

        @Override
        public void abortSession(String sessionId) throws OpencodeException {
            abortCalls.add(sessionId);
            if (abortFailure != null) {
                throw abortFailure;
            }
        }

        @Override
        public void log(String service, String level, String message, Map<String, Object> extra) {
            // not needed here
        }
    }
}
