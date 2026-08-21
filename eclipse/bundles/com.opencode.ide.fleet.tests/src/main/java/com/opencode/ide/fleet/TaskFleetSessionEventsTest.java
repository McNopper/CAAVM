package com.opencode.ide.fleet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.tasks.Task;
import com.opencode.ide.tasks.TaskStore;

/**
 * {@link TaskFleet} with an injected {@link SessionEvents} (SSE-style
 * completion instead of the runner's polling): idle + assistant reply merges
 * and bookkeeps, an idle without a reply waits for the NEXT idle, a
 * timeout/failing seam blocks the ticket, and the default client constructor
 * keeps the polling behavior. Reuses the in-memory fakes and the
 * {@link TaskFleetTest} fixtures.
 */
public class TaskFleetSessionEventsTest {

    private static final Path REPO = Path.of("repo");
    private static final String PROJECT = "p";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private TaskStore store;
    private FakeClient client;
    private FakeWorktreeManager worktrees;

    @Before
    public void setUp() {
        store = new TaskStore(tmp.getRoot().toPath().resolve("tasks"));
        client = new FakeClient();
        worktrees = new FakeWorktreeManager();
    }

    /** Scriptable fake of the seam: polls scripted results, then fallback. */
    private static final class FakeSessionEvents implements SessionEvents {
        final List<String> awaitedSessions = new ArrayList<>();
        private final ArrayDeque<Boolean> scripted = new ArrayDeque<>();
        private final boolean fallback;
        /** Optional hook, invoked on each awaitIdle after recording the session. */
        Runnable onAwait;

        FakeSessionEvents(boolean fallback, Boolean... scripted) {
            this.fallback = fallback;
            Collections.addAll(this.scripted, scripted);
        }

        @Override
        public boolean awaitIdle(String sessionId, Duration timeout) {
            awaitedSessions.add(sessionId);
            if (onAwait != null) {
                onAwait.run();
            }
            Boolean result = scripted.poll();
            return result != null ? result : fallback;
        }
    }

    private static final class ThrowingSessionEvents implements SessionEvents {
        @Override
        public boolean awaitIdle(String sessionId, Duration timeout) throws OpencodeException {
            throw new OpencodeException("event stream broken");
        }
    }

    private String sprintTicket(String role) {
        Task t = store.create(PROJECT, new TaskStore.CreateSpec(
                "Fix the widget", "Do the thing.", "task", role, "high", 3,
                List.of("ac one", "ac two"), List.of(), null, "H1"));
        store.planSprint(PROJECT, "S-01", List.of(t.id), "goal");
        return t.id;
    }

    private void sessionCompletes() {
        client.replyOnSend = "done";
        client.sessionType = "idle";
    }

    private TaskFleet fleet(SessionEvents events) {
        return new TaskFleet(new FleetRunner(client, worktrees, () -> { }),
                store, new RoleAgents(), events);
    }

    @Test
    public void idleSignalDrivesCompletionAndStoreBookkeeping() {
        String id = sprintTicket("developer");
        sessionCompletes();
        FakeSessionEvents events = new FakeSessionEvents(true);
        TaskFleet fleet = fleet(events);

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.MERGED, job.state());
        assertEquals("the seam was asked for the job's session",
                List.of("ses_1"), events.awaitedSessions);
        Task after = store.get(PROJECT, id);
        assertEquals("in-review", after.status);
        assertFalse("no blocker on success", after.blocked);
        assertTrue(after.artifacts.stream().anyMatch(a ->
                "git".equals(a.kind()) && ("opencode/" + id).equals(a.ref())));
        assertEquals(List.of(id), worktrees.mergedTaskIds);
    }

    @Test
    public void idleWithoutAssistantReplyWaitsForTheNextIdle() {
        String id = sprintTicket("developer");
        client.sessionType = "idle"; // idle status, but no assistant reply yet
        FakeSessionEvents events = new FakeSessionEvents(false, true, true);
        events.onAwait = () -> {
            if (events.awaitedSessions.size() == 2) {
                client.completeSession(events.awaitedSessions.get(0), "done");
            }
        };
        TaskFleet fleet = fleet(events);

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.MERGED, job.state());
        assertEquals("first idle had no reply, so a second idle was awaited",
                2, events.awaitedSessions.size());
        assertEquals("in-review", store.get(PROJECT, id).status);
    }

    @Test
    public void seamTimeoutBlocksTheTicket() {
        String id = sprintTicket("pm");
        TaskFleet fleet = fleet(new FakeSessionEvents(false));

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.FAILED, job.state());
        assertTrue(job.detail(), job.detail().contains("timeout"));
        Task after = store.get(PROJECT, id);
        assertTrue(after.blocked);
        assertTrue(after.blocker, after.blocker.contains("timeout"));
        assertTrue("no merge must be attempted", worktrees.mergedTaskIds.isEmpty());
    }

    @Test
    public void seamFailureBlocksTheTicket() {
        String id = sprintTicket("developer");
        TaskFleet fleet = fleet(new ThrowingSessionEvents());

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.FAILED, job.state());
        assertTrue(job.detail(), job.detail().contains("event stream broken"));
        Task after = store.get(PROJECT, id);
        assertTrue(after.blocked);
        assertTrue(after.blocker, after.blocker.contains("event stream broken"));
    }

    @Test
    public void defaultClientConstructorKeepsPollingCompletion() {
        String id = sprintTicket("developer");
        sessionCompletes();
        TaskFleet fleet = new TaskFleet(client, worktrees, store);

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.MERGED, job.state());
        assertEquals("in-review", store.get(PROJECT, id).status);
    }
}
