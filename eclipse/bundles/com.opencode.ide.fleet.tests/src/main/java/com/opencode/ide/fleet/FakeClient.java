package com.opencode.ide.fleet;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencode.ide.client.ChatRequest;
import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.ChatMessageInfo;
import com.opencode.ide.client.model.ChatPart;
import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SessionStatus;
import com.opencode.ide.client.model.SessionTodo;
import com.opencode.ide.client.McpServerConfig;

/**
 * In-memory fake of {@link OpencodeClient} for the fleet tests (no HTTP).
 * Shared by {@link FleetRunnerTest} and {@link TaskFleetTest}; set
 * {@link #onSessionCreated} to observe the world at submit time.
 */
final class FakeClient implements OpencodeClient {

    final List<String> createdTitles = new ArrayList<>();
    final List<Path> sessionDirectories = new ArrayList<>();
    final List<ChatRequest> sentRequests = new ArrayList<>();
    final Map<String, List<ChatEntry>> messagesBySession = new HashMap<>();
    /** Served by {@link #getSessionTodos(String)} for every session (settable). */
    final List<SessionTodo> sessionTodos = new ArrayList<>();

    String sessionType = "busy";
    String replyOnSend;
    boolean failSessionCreation;
    /** When set, {@link #getMessages(String)} fails - used to prove telemetry is best-effort. */
    boolean failGetMessages;
    /** Optional hook, invoked after each successful session creation. */
    Runnable onSessionCreated;
    /** Optional hook, invoked inside sendMessage (blocks the send while it runs). */
    Runnable blockOnSend;

    private int sessionCounter;
    private int messageCounter;

    void addEntry(String sessionId, String role, String text) {
        messagesBySession.get(sessionId).add(entry(sessionId, role, text));
    }

    void completeSession(String sessionId, String reply) {
        sessionType = "idle";
        addEntry(sessionId, "assistant", reply);
    }

    private ChatEntry entry(String sessionId, String role, String text) {
        ChatMessageInfo info = new ChatMessageInfo(
                "msg_" + (++messageCounter), sessionId, role,
                null, null, null, null, null, null, null, null, null, null);
        List<ChatPart> parts = (text == null) ? List.of() : List.of(new ChatPart("text", text, null, null));
        return new ChatEntry(info, parts);
    }

    @Override
    public Session createSession(String title, Path directory) throws OpencodeException {
        if (failSessionCreation) {
            throw new OpencodeException("session create failed");
        }
        String id = "ses_" + (++sessionCounter);
        createdTitles.add(title);
        sessionDirectories.add(directory);
        messagesBySession.put(id, new ArrayList<>());
        if (onSessionCreated != null) {
            onSessionCreated.run();
        }
        return new Session(id, "slug", title, null, null, null, null, null, null);
    }

    @Override
    public Map<String, SessionStatus> getSessionStatus() {
        Map<String, SessionStatus> status = new HashMap<>();
        for (String id : messagesBySession.keySet()) {
            status.put(id, new SessionStatus(sessionType));
        }
        return status;
    }

    @Override
    public List<ChatEntry> getMessages(String sessionId) throws OpencodeException {
        if (failGetMessages) {
            throw new OpencodeException("messages fetch failed");
        }
        return messagesBySession.getOrDefault(sessionId, List.of());
    }

    @Override
    public List<SessionTodo> getSessionTodos(String sessionId) {
        return new ArrayList<>(sessionTodos);
    }

    @Override
    public ChatEntry sendMessage(ChatRequest request) {
        if (blockOnSend != null) {
            blockOnSend.run();
        }
        sentRequests.add(request);
        List<ChatEntry> entries = messagesBySession.get(request.sessionId());
        entries.add(entry(request.sessionId(), "user", request.text()));
        if (replyOnSend != null) {
            entries.add(entry(request.sessionId(), "assistant", replyOnSend));
        }
        return entries.get(entries.size() - 1);
    }

    @Override
    public HealthStatus getHealth() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Agent> getAgents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderList getProviders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConfigInfo getConfig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Session> getSessions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerMcp(String name, McpServerConfig config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void log(String service, String level, String message, Map<String, Object> extra) {
        throw new UnsupportedOperationException();
    }
}
