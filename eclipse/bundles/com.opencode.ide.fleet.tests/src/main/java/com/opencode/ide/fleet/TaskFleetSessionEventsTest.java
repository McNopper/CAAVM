package com.opencode.ide.fleet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.opencode.ide.tasks.Task;
import com.opencode.ide.tasks.TaskStore;

/**
 * {@link TaskFleet}'s WATCHDOG completion (2026-08-28 redesign): the prompt
 * POST runs on its own thread and completion is judged by probing (busy flag
 * + last assistant reply) - so a finished session merges even if the POST
 * response is stuck, a prompt failure surfaces fast, and a session with no
 * new messages for the stall threshold is aborted and fails cleanly instead
 * of burning the whole budget. Reuses the in-memory fakes and TaskFleetTest
 * fixtures.
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

    private String sprintTicket(String role) {
        Task t = store.create(PROJECT, new TaskStore.CreateSpec(
                "Fix the widget", "Do the thing.", "task", role, "high", 3,
                List.of("ac one"), List.of(), null, "H1"));
        store.planSprint(PROJECT, "S-01", List.of(t.id), "goal");
        return t.id;
    }

    private void sessionCompletes() {
        client.replyOnSend = "done";
        client.sessionType = "idle";
    }

    private TaskFleet fleet() {
        return new TaskFleet(new FleetRunner(client, worktrees, () -> { }),
                store, new RoleAgents(), null);
    }

    @Test
    public void probeCompletionDrivesMergeAndBookkeeping() {
        String id = sprintTicket("developer");
        sessionCompletes();
        TaskFleet fleet = fleet();

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.MERGED, job.state());
        Task after = store.get(PROJECT, id);
        assertEquals("in-review", after.status);
        assertFalse("no blocker on success", after.blocked);
        assertTrue(after.artifacts.stream().anyMatch(a ->
                "git".equals(a.kind()) && ("opencode/" + id).equals(a.ref())));
        assertEquals(List.of(id), worktrees.mergedTaskIds);
    }

    @Test
    public void idleWithoutAssistantReplyWaitsUntilTheReplyAppears() {
        String id = sprintTicket("developer");
        client.sessionType = "idle"; // idle status, but no assistant reply yet
        AtomicInteger probes = new AtomicInteger();
        FleetRunner runner = new FleetRunner(client, worktrees, () -> {
            if (probes.incrementAndGet() == 1) {
                client.completeSession("ses_1", "done");
            }
        });
        TaskFleet fleet = new TaskFleet(runner, store, new RoleAgents(), null);

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.MERGED, job.state());
        assertEquals("in-review", store.get(PROJECT, id).status);
    }

    @Test
    public void busySessionTimesOutAndBlocksTheTicket() {
        String id = sprintTicket("pm");
        TaskFleet fleet = fleet();

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.FAILED, job.state());
        assertTrue(job.detail(), job.detail().contains("timeout"));
        Task after = store.get(PROJECT, id);
        assertTrue(after.blocked);
        assertTrue(after.blocker, after.blocker.contains("timeout"));
        assertTrue("no merge must be attempted", worktrees.mergedTaskIds.isEmpty());
    }

    @Test
    public void promptFailureBlocksTheTicket() {
        String id = sprintTicket("developer");
        client.blockOnSend = () -> {
            throw new IllegalStateException("event stream broken");
        };
        TaskFleet fleet = fleet();

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.FAILED, job.state());
        assertTrue(job.detail(), job.detail().contains("event stream broken"));
        Task after = store.get(PROJECT, id);
        assertTrue(after.blocked);
        assertTrue(after.blocker, after.blocker.contains("event stream broken"));
    }

    /**
     * The watchdog's reason to exist: a session that streams nothing new for
     * the stall threshold is ABORTED and fails within the threshold - not
     * after the whole budget. Progress resets the clock, so slow workers
     * survive (see {@link #progressResetsTheStallClock()}).
     */
    @Test
    public void stalledSessionIsAbortedAndBlocksTheTicket() {
        String id = sprintTicket("developer");
        client.sessionType = "busy"; // stays busy, messages stay static
        TaskFleet fleet = fleet().withStallTimeout(Duration.ofMillis(50));

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals(FleetJob.State.FAILED, job.state());
        assertTrue(job.detail(), job.detail().contains("stalled"));
        assertTrue("the hung session was aborted", client.aborted.contains("ses_1"));
        Task after = store.get(PROJECT, id);
        assertTrue(after.blocked);
        assertTrue(after.blocker, after.blocker.contains("stalled"));
    }

    @Test
    public void progressResetsTheStallClock() {
        String id = sprintTicket("developer");
        client.sessionType = "busy"; // busy the whole time, never completes
        AtomicInteger probes = new AtomicInteger();
        FleetRunner runner = new FleetRunner(client, worktrees, () -> {
            // a new message arrives on every probe: the worker is progressing
            client.addEntry("ses_1", "assistant", "progress " + probes.incrementAndGet());
        });
        TaskFleet fleet = new TaskFleet(runner, store, new RoleAgents(), null)
                .withStallTimeout(Duration.ofMillis(200));

        FleetJob job = fleet.launch(PROJECT, id, REPO, TIMEOUT);

        assertEquals("busy but progressing workers are never stall-killed -"
                + " they run to the budget", FleetJob.State.FAILED, job.state());
        assertTrue(job.detail(), job.detail().contains("timeout"));
        assertTrue("never aborted", client.aborted.isEmpty());
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
