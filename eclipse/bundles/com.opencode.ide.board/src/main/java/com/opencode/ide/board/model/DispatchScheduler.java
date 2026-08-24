package com.opencode.ide.board.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.opencode.ide.tasks.StageReadiness;
import com.opencode.ide.tasks.Task;

/**
 * The H6 background dispatch loop (the self-draining scheduler): ticks
 * {@link AutoDispatch} over one sprint until it drains. SWT-free, pure
 * orchestration over injected seams — the sprint scope is the
 * {@code sprintTasks} supplier's contract (the caller hands in an
 * ALREADY sprint-scoped list; the scheduler never sees out-of-sprint
 * tasks), and every seam is re-read per tick so tickets added to the
 * sprint drain automatically. Nothing here ever throws: a failing
 * supplier aborts the tick with an empty plan, a failing launch is
 * logged and skipped (the remaining launches of the tick still run),
 * and the loop keeps serving.
 *
 * <p>Launch-echo guard: the real launcher publishes its RUNNING rows
 * synchronously, so the running set catches up immediately in the normal
 * case — but a lagging {@code runningTaskIds} supplier would re-admit a
 * just-launched ticket on the next tick. Every launch attempt is
 * therefore remembered by id with the injectable {@link Clock} and held
 * back for {@link #LAUNCH_HOLD}; the hold is belt-and-braces, not a
 * second source of truth. The returned plan reports what actually went
 * out: held-back ids join the policy's skips with their own reason.</p>
 *
 * <p>Lifecycle: {@link #start(Duration)} runs {@link #tick()} immediately
 * and then periodically on a single daemon thread ({@code board-dispatch});
 * {@link #stop()} is idempotent, prevents further ticks and shuts the
 * executor down (also for an injected one — ownership transfers on
 * start). Direct {@link #tick()} calls work at any time and are the
 * deterministic seam the tests drive.</p>
 */
public final class DispatchScheduler {

    /** How long a launched ticket is held back from re-admission even when the running set has not caught up. */
    public static final Duration LAUNCH_HOLD = Duration.ofMinutes(2);

    private static final Logger LOG = Logger.getLogger(DispatchScheduler.class.getName());

    private final AutoDispatch policy;
    private final Supplier<List<Task>> sprintTasks;
    private final Supplier<CostOverview> cost;
    private final Supplier<Set<String>> runningTaskIds;
    private final Consumer<String> launch;
    private final Clock clock;
    private final Map<String, Instant> launchedAt = new ConcurrentHashMap<>();

    /** Guards {@link #executor} and {@link #running}; the tick itself runs unlocked. */
    private final Object lifecycleLock = new Object();
    private ScheduledExecutorService executor;
    private volatile boolean running;

    /**
     * @param policy          the dispatch policy ticked each round
     * @param sprintTasks     the CURRENT sprint's tickets (already
     *                        sprint-scoped by the caller — the guardrail);
     *                        re-read every tick
     * @param cost            the cost overview the budget counts; re-read
     *                        every tick
     * @param runningTaskIds  ticket ids with a live fleet job; re-read
     *                        every tick
     * @param launch          launches one admitted ticket id (isolated —
     *                        a throw is logged, never propagated)
     * @param clock           times the launch-echo hold;
     *                        {@code null} reads as the system clock
     */
    public DispatchScheduler(AutoDispatch policy, Supplier<List<Task>> sprintTasks,
            Supplier<CostOverview> cost, Supplier<Set<String>> runningTaskIds,
            Consumer<String> launch, Clock clock) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.sprintTasks = Objects.requireNonNull(sprintTasks, "sprintTasks");
        this.cost = Objects.requireNonNull(cost, "cost");
        this.runningTaskIds = Objects.requireNonNull(runningTaskIds, "runningTaskIds");
        this.launch = Objects.requireNonNull(launch, "launch");
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * One dispatch wave, never throws: evaluates readiness over the
     * sprint supplier's tickets, delegates to
     * {@link AutoDispatch#plan(List, Map, CostOverview, Set)} with the
     * current running set, holds back recently launched ids, then
     * launches the rest through the consumer.
     *
     * @return the plan as executed — {@code launch} lists what really
     *         went out this tick, {@code skipped} everyone else with a
     *         reason
     */
    public AutoDispatch.DispatchPlan tick() {
        List<Task> sprint;
        CostOverview overview;
        Set<String> runningIds;
        try {
            sprint = sprintTasks.get();
            overview = cost.get();
            runningIds = runningTaskIds.get();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "dispatch tick skipped: data supplier failed: " + e.getMessage(), e);
            return new AutoDispatch.DispatchPlan(List.of(), List.of());
        }
        AutoDispatch.DispatchPlan planned =
                policy.plan(sprint, StageReadiness.evaluate(sprint), overview, runningIds);
        Instant now = clock.instant();
        pruneLaunched(now);
        List<String> launchedNow = new ArrayList<>();
        List<AutoDispatch.Skip> skipped = new ArrayList<>(planned.skipped());
        for (String id : planned.launch()) {
            if (isHeld(id, now)) {
                skipped.add(new AutoDispatch.Skip(id,
                        "recently launched; waiting for the fleet job to reach the running set"));
            } else {
                launchedNow.add(id);
            }
        }
        for (String id : launchedNow) {
            launchedAt.put(id, now);
            try {
                launch.accept(id);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "dispatch launch of " + id + " failed: " + e.getMessage(), e);
            }
        }
        return new AutoDispatch.DispatchPlan(List.copyOf(launchedNow), List.copyOf(skipped));
    }

    /** @return true while the background loop is scheduled. */
    public boolean isRunning() {
        return running;
    }

    /**
     * Runs one tick immediately, then every {@code period}, on a fresh
     * single-thread daemon executor ({@code board-dispatch}); replaces a
     * previous start.
     */
    public void start(Duration period) {
        start(period, Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "board-dispatch");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /**
     * Test seam: schedules the loop on the given executor (ownership
     * transfers — {@link #stop()} shuts it down).
     *
     * @throws IllegalArgumentException when {@code period} is null, zero or negative
     */
    public void start(Duration period, ScheduledExecutorService scheduler) {
        if (period == null || period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("period must be > 0: " + period);
        }
        Objects.requireNonNull(scheduler, "scheduler");
        synchronized (lifecycleLock) {
            stop();
            executor = scheduler;
            running = true;
            scheduler.scheduleAtFixedRate(this::runTick, 0, period.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** Stops the loop; idempotent. Prevents further ticks and shuts the executor down. */
    public void stop() {
        synchronized (lifecycleLock) {
            running = false;
            ScheduledExecutorService current = executor;
            executor = null;
            if (current != null) {
                current.shutdown();
            }
        }
    }

    /**
     * One scheduled tick: never launches once stopped, and a throwing tick
     * is logged, not propagated — the schedule keeps serving.
     */
    private void runTick() {
        if (!running) {
            return;
        }
        try {
            tick();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "dispatch tick threw (kept serving): " + e.getMessage(), e);
        }
    }

    private boolean isHeld(String id, Instant now) {
        Instant at = launchedAt.get(id);
        return at != null && Duration.between(at, now).compareTo(LAUNCH_HOLD) < 0;
    }

    private void pruneLaunched(Instant now) {
        launchedAt.entrySet()
                .removeIf(e -> Duration.between(e.getValue(), now).compareTo(LAUNCH_HOLD) >= 0);
    }
}
