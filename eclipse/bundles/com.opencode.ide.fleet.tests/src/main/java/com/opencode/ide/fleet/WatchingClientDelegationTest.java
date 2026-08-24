package com.opencode.ide.fleet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.opencode.ide.client.ChatRequest;
import com.opencode.ide.client.McpServerConfig;
import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.ChatMessageInfo;
import com.opencode.ide.client.model.ChatPart;
import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.OpencodeEvent;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SessionStatus;
import com.opencode.ide.client.model.ShellResult;
import com.opencode.ide.client.model.VcsInfo;

/**
 * Guard + delegation tests for the watching client wrapper that
 * {@link FleetPermissionBridge#watching(OpencodeClient)} hands out: every
 * public {@link OpencodeClient} method must be overridden by the wrapper
 * (reflection), so the next client method forces a conscious update here
 * instead of silently hitting an interface default that throws or returns
 * empty. The interception specials ({@code createSession} overloads) are
 * overridden too - with registration - so the allowlist is empty.
 */
public class WatchingClientDelegationTest {

    /**
     * Wrapper methods intentionally NOT overridden (interception specials).
     * Empty today; add a name here only with a reason when a method must not
     * delegate mechanically.
     */
    private static final Set<String> ALLOWED_NOT_OVERRIDDEN = Set.of();

    @Test
    public void watchingClientOverridesEveryPublicClientMethod() {
        OpencodeClient watched = watched(new RecordingClient());

        List<String> missing = new ArrayList<>();
        for (Method method : OpencodeClient.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class
                    || ALLOWED_NOT_OVERRIDDEN.contains(method.getName())) {
                continue;
            }
            try {
                watched.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException notOverridden) {
                missing.add(method.getName());
            }
        }

        assertTrue("WatchingClient must delegate: " + missing, missing.isEmpty());
    }

    @Test
    public void runCommandDelegatesArgumentsAndReturnsTheDelegateReply() throws Exception {
        RecordingClient delegate = new RecordingClient();
        OpencodeClient watched = watched(delegate);

        ChatEntry reply = watched.runCommand("ses_1", "verify", List.of("--fast"));

        assertSame("returns the delegate's reply", delegate.commandReply, reply);
        assertEquals(List.of("ses_1|verify|[--fast]"), delegate.commandCalls);
    }

    @Test
    public void respondToPermissionDelegatesArgumentsAndReturnsTheDelegateValue() throws Exception {
        RecordingClient delegate = new RecordingClient();
        OpencodeClient watched = watched(delegate);

        boolean accepted = watched.respondToPermission("ses_1", "per_9", "once", true);

        assertTrue("returns the delegate's value", accepted);
        assertEquals(List.of("ses_1|per_9|once|remember=true"), delegate.permissionReplies);
    }

    @Test
    public void abortSessionDelegatesInsteadOfHittingTheThrowingDefault() throws Exception {
        RecordingClient delegate = new RecordingClient();
        OpencodeClient watched = watched(delegate);

        watched.abortSession("ses_1");

        assertEquals(List.of("ses_1"), delegate.aborts);
    }

    @Test
    public void defaultValueMethodsDelegateInsteadOfReturningInterfaceDefaults() throws Exception {
        RecordingClient delegate = new RecordingClient();
        OpencodeClient watched = watched(delegate);

        assertSame(delegate.vcs, watched.getVcsInfo());
        assertSame(delegate.fileContent, watched.getFileContent("README.md"));
    }

    @Test
    public void createSessionOverloadsKeepTheRegistrationInterception() throws Exception {
        PermissionQueue queue = new PermissionQueue(null);
        FleetPermissionBridge bridge = new FleetPermissionBridge(queue);
        OpencodeClient watched = bridge.watching(new RecordingClient());

        Session oneArg = watched.createSession("title");
        Session twoArg = watched.createSession("title", Path.of("worktree"));

        bridge.onEvent(asked(oneArg.id()));
        bridge.onEvent(asked(twoArg.id()));

        assertEquals("both overloads registered their session", 2, queue.pendingCount());
    }

    private static OpencodeEvent asked(String sessionId) {
        JsonObject properties = new JsonObject();
        properties.addProperty("sessionID", sessionId);
        properties.addProperty("id", "per_" + sessionId);
        properties.addProperty("permission", "bash");
        return new OpencodeEvent("permission.asked", properties);
    }

    private static OpencodeClient watched(OpencodeClient delegate) {
        return new FleetPermissionBridge(new PermissionQueue(null)).watching(delegate);
    }

    /**
     * Recording fake: the sampled methods capture their calls and return
     * distinguishing values; the rest is never invoked by these tests and
     * throws like {@link FakeClient}'s stubs.
     */
    private static final class RecordingClient implements OpencodeClient {

        final List<String> commandCalls = new ArrayList<>();
        final List<String> permissionReplies = new ArrayList<>();
        final List<String> aborts = new ArrayList<>();
        final ChatEntry commandReply = entry("command done");
        final VcsInfo vcs = new VcsInfo("main", "origin");
        final String fileContent = "# readme";

        private int sessionCounter;

        @Override
        public ChatEntry runCommand(String sessionId, String command, List<String> arguments) {
            commandCalls.add(sessionId + "|" + command + "|" + arguments);
            return commandReply;
        }

        @Override
        public boolean respondToPermission(String sessionId, String permissionId, String response, boolean remember) {
            permissionReplies.add(sessionId + "|" + permissionId + "|" + response + "|remember=" + remember);
            return true;
        }

        @Override
        public void abortSession(String sessionId) {
            aborts.add(sessionId);
        }

        @Override
        public VcsInfo getVcsInfo() {
            return vcs;
        }

        @Override
        public String getFileContent(String path) {
            return fileContent;
        }

        @Override
        public Session createSession(String title, Path directory) {
            return new Session("ses_" + (++sessionCounter), "slug", title, null, null, null, null, null, null);
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
        public Map<String, SessionStatus> getSessionStatus() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerMcp(String name, McpServerConfig config) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ChatEntry> getMessages(String sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChatEntry sendMessage(ChatRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(String service, String level, String message, Map<String, Object> extra) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ShellResult runShell(String sessionId, String agent, String command) {
            throw new UnsupportedOperationException();
        }
    }

    private static ChatEntry entry(String text) {
        ChatMessageInfo info = new ChatMessageInfo(
                "msg_1", "ses_1", "assistant",
                null, null, null, null, null, null, null, null, null, null);
        return new ChatEntry(info, List.of(new ChatPart("text", text, null, null)));
    }
}
