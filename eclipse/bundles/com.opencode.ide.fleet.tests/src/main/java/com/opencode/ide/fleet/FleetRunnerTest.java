package com.opencode.ide.fleet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

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
import com.opencode.ide.git.MergeResult;
import com.opencode.ide.git.Worktree;
import com.opencode.ide.git.WorktreeManager;
import com.opencode.ide.git.WorktreeStatus;
import com.opencode.ide.client.McpServerConfig;

/**
 * Unit tests for {@link FleetRunner} against in-memory fakes of
 * {@link OpencodeClient} and {@link WorktreeManager} (no git, no HTTP).
 */
public class FleetRunnerTest {

    private static final Path REPO = Path.of("repo");

    private final FakeClient client = new FakeClient();
    private final FakeWorktreeManager worktrees = new FakeWorktreeManager();

    private static FleetTask task(String agent, String model) {
        return new FleetTask("t1", "Fleet task", "do the thing", agent, model, REPO);
    }

    private FleetRunner runner() {
        return new FleetRunner(client, worktrees, () -> { });
    }

    @Test
    public void submitCreatesSessionInWorktreeAndSendsPrompt() throws Exception {
        client.replyOnSend = "done";
        FleetRunner runner = runner();

        FleetJob job = runner.submit(task("build", "anthropic/claude"));

        assertEquals(FleetJob.State.RUNNING, job.state());
        assertNull(job.detail());
        assertEquals("t1", job.taskId());
        assertEquals("ses_1", job.sessionId());
        assertEquals(REPO.resolve(".git/opencode-fleet/t1"), job.worktree());
        assertEquals(List.of(REPO.resolve(".git/opencode-fleet/t1")), client.sessionDirectories);
        assertEquals(List.of("Fleet task"), client.createdTitles);
        assertEquals(1, client.sentRequests.size());
        ChatRequest request = client.sentRequests.get(0);
        assertEquals("ses_1", request.sessionId());
        assertEquals("do the thing", request.text());
        assertEquals("build", request.agent());
        assertEquals("anthropic", request.providerId());
        assertEquals("claude", request.modelId());
    }

    @Test
    public void submitUsesServerDefaultsForNullAgentAndModel() {
        FleetJob job = runner().submit(task(null, null));

        assertEquals(FleetJob.State.RUNNING, job.state());
        ChatRequest request = client.sentRequests.get(0);
        assertNull(request.agent());
        assertFalse(request.hasModel());
    }

    @Test
    public void submitFailureAfterWorktreeCreationKeepsWorktreeForPostMortem() {
        client.failSessionCreation = true;

        FleetJob job = runner().submit(task(null, null));

        assertEquals(FleetJob.State.FAILED, job.state());
        assertNull(job.sessionId());
        assertTrue(job.detail(), job.detail().contains("session create failed"));
        assertEquals(REPO.resolve(".git/opencode-fleet/t1"), job.worktree());
        assertEquals(List.of("t1"), worktrees.createdTaskIds);
        assertTrue("no message must be sent", client.sentRequests.isEmpty());
    }

    @Test
    public void isCompleteRequiresIdleSessionStatus() throws Exception {
        client.replyOnSend = "done";
        FleetRunner runner = runner();
        FleetJob job = runner.submit(task(null, null));

        client.sessionType = "busy";
        assertFalse(runner.isComplete(job));

        client.sessionType = "idle";
        assertTrue(runner.isComplete(job));
    }

    @Test
    public void isCompleteRequiresAssistantReply() throws Exception {
        FleetRunner runner = runner();
        FleetJob job = runner.submit(task(null, null));

        client.sessionType = "idle";
        assertFalse("only the user message exists", runner.isComplete(job));
    }

    @Test
    public void isCompleteRequiresNonEmptyAssistantText() throws Exception {
        FleetRunner runner = runner();
        FleetJob job = runner.submit(task(null, null));

        client.sessionType = "idle";
        client.addEntry(job.sessionId(), "assistant", "");
        assertFalse(runner.isComplete(job));
    }

    @Test
    public void awaitCompletionReturnsCompletedJob() throws Exception {
        client.replyOnSend = "done";
        client.sessionType = "idle";
        FleetJob job = runner().submit(task(null, null));

        job = runner().awaitCompletion(job, Duration.ofSeconds(5));

        assertEquals(FleetJob.State.COMPLETED, job.state());
        assertNull(job.detail());
    }

    @Test
    public void awaitCompletionPollsUntilSessionCompletes() throws Exception {
        FleetJob submitted = runner().submit(task(null, null));
        final String sessionId = submitted.sessionId();

        FleetRunner polling = new FleetRunner(client, worktrees,
                () -> client.completeSession(sessionId, "finished"));

        FleetJob job = polling.awaitCompletion(submitted, Duration.ofSeconds(5));

        assertEquals(FleetJob.State.COMPLETED, job.state());
    }

    @Test
    public void awaitCompletionTimesOutToFailed() throws Exception {
        FleetJob job = runner().submit(task(null, null));

        job = runner().awaitCompletion(job, Duration.ofMillis(20));

        assertEquals(FleetJob.State.FAILED, job.state());
        assertTrue(job.detail(), job.detail().contains("timeout"));
        assertTrue(worktrees.mergedTaskIds.isEmpty());
    }

    @Test
    public void mergeBackMergesAndCallsManagerWithTaskId() throws Exception {
        client.replyOnSend = "done";
        client.sessionType = "idle";
        FleetRunner runner = runner();
        FleetJob job = runner.submit(task(null, null));
        job = runner.awaitCompletion(job, Duration.ofSeconds(5));

        job = runner.mergeBack(job);

        assertEquals(FleetJob.State.MERGED, job.state());
        assertNull(job.detail());
        assertEquals(List.of("t1"), worktrees.mergedTaskIds);
        assertEquals(List.of(REPO), worktrees.mergedRepoRoots);
    }

    @Test
    public void mergeBackConflictFailsWithConflictedFiles() throws Exception {
        client.replyOnSend = "done";
        client.sessionType = "idle";
        FleetRunner runner = runner();
        FleetJob job = runner.awaitCompletion(runner.submit(task(null, null)), Duration.ofSeconds(5));
        worktrees.nextMergeResult = new MergeResult(false, List.of("src/A.java", "README.md"), "CONFLICT");

        job = runner.mergeBack(job);

        assertEquals(FleetJob.State.FAILED, job.state());
        assertTrue(job.detail(), job.detail().contains("src/A.java"));
        assertTrue(job.detail(), job.detail().contains("README.md"));
    }

    @Test
    public void mergeBackRejectsJobThatIsNotCompleted() {
        FleetJob job = runner().submit(task(null, null));

        try {
            runner().mergeBack(job);
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("RUNNING"));
        }
        assertTrue(worktrees.mergedTaskIds.isEmpty());
    }

    private static final class FakeClient implements OpencodeClient {

        final List<String> createdTitles = new ArrayList<>();
        final List<Path> sessionDirectories = new ArrayList<>();
        final List<ChatRequest> sentRequests = new ArrayList<>();
        final Map<String, List<ChatEntry>> messagesBySession = new HashMap<>();

        String sessionType = "busy";
        String replyOnSend;
        boolean failSessionCreation;
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
            List<ChatPart> parts = (text == null) ? List.of() : List.of(new ChatPart("text", text));
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
            return new Session(id, "slug", title, null, null, null, null, null);
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
        public List<ChatEntry> getMessages(String sessionId) {
            return messagesBySession.getOrDefault(sessionId, List.of());
        }

        @Override
        public ChatEntry sendMessage(ChatRequest request) {
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

    private static final class FakeWorktreeManager implements WorktreeManager {

        final List<String> createdTaskIds = new ArrayList<>();
        final List<String> mergedTaskIds = new ArrayList<>();
        final List<Path> mergedRepoRoots = new ArrayList<>();
        MergeResult nextMergeResult = new MergeResult(true, List.of(), "merged");

        @Override
        public Worktree create(Path repoRoot, String taskId) {
            createdTaskIds.add(taskId);
            return new Worktree(taskId, repoRoot.resolve(".git/opencode-fleet").resolve(taskId),
                    "opencode/" + taskId);
        }

        @Override
        public MergeResult mergeBack(Path repoRoot, String taskId) {
            mergedTaskIds.add(taskId);
            mergedRepoRoots.add(repoRoot);
            return nextMergeResult;
        }

        @Override
        public List<Worktree> list(Path repoRoot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Worktree> find(Path repoRoot, String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(Path repoRoot, String taskId, boolean force) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorktreeStatus status(Path repoRoot, String taskId) {
            throw new UnsupportedOperationException();
        }
    }
}
