package com.opencode.ide.fleet;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.opencode.ide.client.ChatRequest;
import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SessionStatus;
import com.opencode.ide.client.model.ShellResult;
import com.opencode.ide.git.MergeResult;
import com.opencode.ide.git.Worktree;
import com.opencode.ide.git.WorktreeManager;

/**
 * Headless one-task-per-worktree orchestration: creates the task worktree,
 * runs the task's optional {@link Bootstrap} shell command in a
 * directory-scoped opencode session, sends the prompt, polls for completion,
 * and merges the task branch back into the main worktree.
 *
 * <p>Pure Java, no Eclipse/OSGi - the later Fleet view drives this engine.
 * Merge-back is not synchronized internally; callers must serialize it (see
 * {@link WorktreeManager#mergeBack}).</p>
 */
public class FleetRunner {

    private static final Logger LOG = Logger.getLogger(FleetRunner.class.getName());
    private static final long DEFAULT_POLL_MILLIS = 1000;
    private static final int BOOTSTRAP_OUTPUT_LIMIT = 200;

    private final OpencodeClient client;
    private final WorktreeManager worktrees;
    private final Runnable sleeper;
    private final Map<String, FleetTask> tasks = new HashMap<>();

    public FleetRunner(OpencodeClient client, WorktreeManager worktrees) {
        this(client, worktrees, FleetRunner::sleepPollInterval);
    }

    /**
     * @param sleeper invoked between completion polls; inject a no-op to make
     *                {@link #awaitCompletion} run instantly in tests
     */
    public FleetRunner(OpencodeClient client, WorktreeManager worktrees, Runnable sleeper) {
        this.client = client;
        this.worktrees = worktrees;
        this.sleeper = sleeper;
    }

    /**
     * Creates the task worktree ({@code opencode/<taskId>} via the
     * {@link WorktreeManager}), opens a session scoped to it, runs the task's
     * optional {@link Bootstrap} command, and sends the prompt. On client
     * failure after worktree creation the returned job is {@code FAILED}; the
     * worktree is deliberately kept (see {@link FleetJob#worktree}) for
     * post-mortem inspection.
     */
    public FleetJob submit(FleetTask task) {
        Worktree worktree = worktrees.create(task.baseWorktree(), task.taskId());
        try {
            Session session = client.createSession(task.title(), worktree.path());
            runBootstrap(session.id(), task.bootstrap());
            client.sendMessage(chatRequest(session.id(), task));
            tasks.put(task.taskId(), task);
            return new FleetJob(task.taskId(), session.id(), worktree.path(), FleetJob.State.RUNNING, null);
        } catch (OpencodeException e) {
            return new FleetJob(task.taskId(), null, worktree.path(), FleetJob.State.FAILED, e.getMessage());
        }
    }

    /**
     * Best-effort pre-prompt bootstrap: runs the task's optional shell
     * command in the new session ({@link OpencodeClient#runShell} - the call
     * blocks until the process exits, possibly for minutes, on this launch
     * thread like the prompt itself). A transport failure or an error status
     * is logged and the launch proceeds with the prompt - the bootstrap is a
     * convenience (e.g. {@code npm install}), never a gate. Never throws; a
     * {@code null} or blank command is no bootstrap at all.
     */
    private void runBootstrap(String sessionId, Bootstrap bootstrap) {
        if (bootstrap == null || bootstrap.command() == null || bootstrap.command().isBlank()) {
            return;
        }
        try {
            ShellResult result = client.runShell(sessionId, bootstrap.agent(), bootstrap.command());
            String status = result == null || result.status() == null ? "unknown" : result.status();
            String output = summarize(result == null ? null : result.output());
            if (status.toLowerCase(Locale.ROOT).contains("error")) {
                LOG.log(Level.WARNING, "fleet bootstrap '" + bootstrap.command() + "' in session "
                        + sessionId + " reported status " + status + "; proceeding with the prompt. output: "
                        + output);
            } else {
                LOG.fine(() -> "fleet bootstrap '" + bootstrap.command() + "' in session " + sessionId
                        + " finished with status " + status + ", output: " + output);
            }
        } catch (OpencodeException | RuntimeException e) {
            LOG.log(Level.WARNING, "fleet bootstrap '" + bootstrap.command() + "' in session "
                    + sessionId + " failed; proceeding with the prompt", e);
        }
    }

    /** Flattens and truncates a bootstrap output for the log summary. */
    private static String summarize(String output) {
        String flat = output == null ? "" : output.strip().replace('\n', ' ');
        return flat.length() <= BOOTSTRAP_OUTPUT_LIMIT ? flat
                : flat.substring(0, BOOTSTRAP_OUTPUT_LIMIT) + "...";
    }

    /**
     * Completion check: the session reports {@code idle} and its last message
     * is an assistant reply with non-empty text.
     */
    public boolean isComplete(FleetJob job) throws OpencodeException {
        SessionStatus status = client.getSessionStatus().get(job.sessionId());
        if (status == null || !"idle".equals(status.type())) {
            return false;
        }
        List<ChatEntry> messages = client.getMessages(job.sessionId());
        if (messages.isEmpty()) {
            return false;
        }
        ChatEntry last = messages.get(messages.size() - 1);
        return last.info() != null
                && "assistant".equals(last.info().role())
                && !last.text().isBlank();
    }

    /**
     * Polls {@link #isComplete} until true or the timeout elapses.
     *
     * @return the job as {@code COMPLETED}, or {@code FAILED} with a timeout
     *         detail
     */
    public FleetJob awaitCompletion(FleetJob job, Duration timeout) throws OpencodeException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!isComplete(job)) {
            if (System.nanoTime() - deadline >= 0) {
                return withState(job, FleetJob.State.FAILED,
                        "timeout after " + timeout + " awaiting session " + job.sessionId());
            }
            sleeper.run();
        }
        return withState(job, FleetJob.State.COMPLETED, null);
    }

    /**
     * Merges a {@code COMPLETED} job's task branch back into the main
     * worktree.
     *
     * @return the job as {@code MERGED}, or {@code FAILED} with the conflicted
     *         files in the detail (the merge itself is aborted cleanly by the
     *         {@link WorktreeManager})
     */
    public FleetJob mergeBack(FleetJob job) {
        if (job.state() != FleetJob.State.COMPLETED) {
            throw new IllegalStateException("mergeBack requires a COMPLETED job, got " + job.state());
        }
        FleetTask task = tasks.get(job.taskId());
        if (task == null) {
            throw new IllegalStateException("unknown task " + job.taskId());
        }
        MergeResult result = worktrees.mergeBack(task.baseWorktree(), job.taskId());
        if (result.merged()) {
            return withState(job, FleetJob.State.MERGED, null);
        }
        return withState(job, FleetJob.State.FAILED,
                "merge conflicts: " + String.join(", ", result.conflictedFiles()));
    }

    private static ChatRequest chatRequest(String sessionId, FleetTask task) {
        ChatRequest request = ChatRequest.of(sessionId, task.prompt());
        if (task.agent() != null && !task.agent().isBlank()) {
            request = request.withAgent(task.agent());
        }
        String model = task.model();
        if (model != null && model.contains("/")) {
            String provider = model.substring(0, model.indexOf('/'));
            String modelId = model.substring(model.indexOf('/') + 1);
            if (!provider.isBlank() && !modelId.isBlank()) {
                request = request.withModel(provider, modelId);
            }
        }
        return request;
    }

    private static FleetJob withState(FleetJob job, FleetJob.State state, String detail) {
        return new FleetJob(job.taskId(), job.sessionId(), job.worktree(), state, detail);
    }

    private static void sleepPollInterval() {
        try {
            Thread.sleep(DEFAULT_POLL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
