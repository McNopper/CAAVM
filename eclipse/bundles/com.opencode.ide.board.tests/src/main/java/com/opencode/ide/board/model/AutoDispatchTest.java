package com.opencode.ide.board.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.opencode.ide.tasks.StageReadiness.Kind;
import com.opencode.ide.tasks.StageReadiness.Readiness;
import com.opencode.ide.tasks.Task;

/**
 * Unit tests for the SWT-free {@link AutoDispatch} policy: validation, the
 * candidate kinds (READY always, STALE only when included), the launch order
 * (STALE before READY — rework before new work — ids in natural order within
 * both groups), the concurrency cap counting already-running jobs, the cost
 * budget boundary (reaching the budget exactly is still admitted; recorded
 * spend counts; unknown spend reads as 0; 0 means unlimited), and the
 * pure-function properties (never throws, never launches non-candidates,
 * same inputs yield the same plan).
 */
public class AutoDispatchTest {

    private static final Readiness READY = new Readiness(Kind.READY, "upstream satisfied");
    private static final Readiness STALE = new Readiness(Kind.STALE, "upstream changed; re-run needed");
    private static final Readiness WAITING = new Readiness(Kind.WAIT_UPSTREAM, "no done/in-review ticket upstream");
    private static final Readiness BLOCKED = new Readiness(Kind.BLOCKED, "ticket is blocked");
    private static final Readiness RUNNING = new Readiness(Kind.RUNNING, "work is in-progress");
    private static final Readiness NOT_APPLICABLE = new Readiness(Kind.NOT_APPLICABLE, "no V stage");

    private static Task ticket(String id) {
        Task t = new Task();
        t.id = id;
        t.stage = "design";
        t.status = "sprint-backlog";
        t.createdAt = Instant.EPOCH;
        t.updatedAt = Instant.EPOCH;
        return t;
    }

    /** An overview whose project total is one actuals comment's cost. */
    private static CostOverview spent(String actualsComment) {
        Task t = ticket("T-cost");
        t.comments.add(new Task.Comment(Instant.EPOCH, "fleet", actualsComment));
        return CostOverview.of(List.of(t));
    }

    private static String reasonOf(AutoDispatch.DispatchPlan plan, String id) {
        return plan.skipped().stream()
                .filter(skip -> id.equals(skip.id()))
                .map(AutoDispatch.Skip::reason)
                .findFirst()
                .orElse("");
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    @Test
    public void ofRejectsConcurrencyBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> AutoDispatch.of(0, 1, true));
        assertThrows(IllegalArgumentException.class, () -> AutoDispatch.of(-1, 1, true));
        assertThrows("the canonical constructor validates too",
                IllegalArgumentException.class, () -> new AutoDispatch(0, 1, true));
    }

    @Test
    public void ofRejectsNegativeOrNaNBudget() {
        assertThrows(IllegalArgumentException.class, () -> AutoDispatch.of(1, -0.01, true));
        assertThrows("NaN is not a budget either",
                IllegalArgumentException.class, () -> AutoDispatch.of(1, Double.NaN, true));
    }

    // ------------------------------------------------------------------
    // Concurrency cap
    // ------------------------------------------------------------------

    @Test
    public void capsByConcurrencyCountingAlreadyRunning() {
        AutoDispatch policy = AutoDispatch.of(3, 0, true);
        List<Task> tasks = List.of(ticket("T-1"), ticket("T-2"), ticket("T-3"));
        Map<String, Readiness> readiness = Map.of(
                "T-1", READY, "T-2", READY, "T-3", READY);
        AutoDispatch.DispatchPlan plan = policy.plan(tasks, readiness, CostOverview.empty(), Set.of("T-9"));
        assertEquals("one of three slots already taken by a job outside the sprint",
                List.of("T-1", "T-2"), plan.launch());
        assertTrue(reasonOf(plan, "T-3"), reasonOf(plan, "T-3").contains("concurrency"));
    }

    @Test
    public void alreadyRunningTicketIsNeverLaunchedEvenWhenReady() {
        AutoDispatch policy = AutoDispatch.of(4, 0, true);
        AutoDispatch.DispatchPlan plan = policy.plan(
                List.of(ticket("T-1")), Map.of("T-1", READY), CostOverview.empty(), Set.of("T-1"));
        assertEquals(List.of(), plan.launch());
        assertTrue(reasonOf(plan, "T-1"), reasonOf(plan, "T-1").contains("already running"));
    }

    @Test
    public void oversubscribedRunningFloorsCapacityAtZero() {
        AutoDispatch policy = AutoDispatch.of(1, 0, true);
        AutoDispatch.DispatchPlan plan = policy.plan(
                List.of(ticket("T-1")), Map.of("T-1", READY), CostOverview.empty(), Set.of("T-8", "T-9"));
        assertEquals(List.of(), plan.launch());
        assertTrue(reasonOf(plan, "T-1"), reasonOf(plan, "T-1").contains("concurrency"));
    }

    // ------------------------------------------------------------------
    // Ordering
    // ------------------------------------------------------------------

    @Test
    public void staleRunsBeforeReady() {
        AutoDispatch policy = AutoDispatch.of(1, 0, true);
        List<Task> tasks = List.of(ticket("T-1"), ticket("T-9"));
        Map<String, Readiness> readiness = Map.of("T-1", READY, "T-9", STALE);
        AutoDispatch.DispatchPlan plan = policy.plan(tasks, readiness, CostOverview.empty(), Set.of());
        assertEquals("rework precedes new work even against ticket order",
                List.of("T-9"), plan.launch());
        assertTrue(reasonOf(plan, "T-1"), reasonOf(plan, "T-1").contains("concurrency"));
    }

    @Test
    public void readyGroupSortsByTicketId() {
        AutoDispatch policy = AutoDispatch.of(3, 0, true);
        List<Task> tasks = List.of(ticket("T-3"), ticket("T-1"), ticket("T-2"));
        Map<String, Readiness> readiness = Map.of("T-1", READY, "T-2", READY, "T-3", READY);
        AutoDispatch.DispatchPlan plan = policy.plan(tasks, readiness, CostOverview.empty(), Set.of());
        assertEquals("input order must not leak into the plan",
                List.of("T-1", "T-2", "T-3"), plan.launch());
    }

    // ------------------------------------------------------------------
    // STALE inclusion
    // ------------------------------------------------------------------

    @Test
    public void includeStaleFalseExcludesStale() {
        AutoDispatch policy = AutoDispatch.of(4, 0, false);
        List<Task> tasks = List.of(ticket("T-1"), ticket("T-9"));
        Map<String, Readiness> readiness = Map.of("T-1", READY, "T-9", STALE);
        AutoDispatch.DispatchPlan plan = policy.plan(tasks, readiness, CostOverview.empty(), Set.of());
        assertEquals(List.of("T-1"), plan.launch());
        assertTrue(reasonOf(plan, "T-9"), reasonOf(plan, "T-9").contains("includeStale"));
    }

    // ------------------------------------------------------------------
    // Cost budget
    // ------------------------------------------------------------------

    @Test
    public void budgetAdmitsUpToAndIncludingTheBoundary() {
        // $0.10 budget, nothing spent, $0.05 per launch: the second launch
        // lands exactly ON the budget and is admitted; the third would exceed
        AutoDispatch policy = AutoDispatch.of(10, 0.10, true);
        List<Task> tasks = List.of(ticket("T-1"), ticket("T-2"), ticket("T-3"));
        Map<String, Readiness> readiness = Map.of("T-1", READY, "T-2", READY, "T-3", READY);
        AutoDispatch.DispatchPlan plan = policy.plan(tasks, readiness, CostOverview.empty(), Set.of());
        assertEquals(List.of("T-1", "T-2"), plan.launch());
        assertTrue(reasonOf(plan, "T-3"), reasonOf(plan, "T-3").contains("budget"));
    }

    @Test
    public void budgetCountsAlreadyRecordedSpend() {
        // $0.05 already recorded + $0.05 per launch against a $0.10 budget: one launch fits
        AutoDispatch policy = AutoDispatch.of(10, 0.10, true);
        CostOverview cost = spent("fleet actuals: cost 0.05 USD");
        List<Task> tasks = List.of(ticket("T-1"), ticket("T-2"));
        Map<String, Readiness> readiness = Map.of("T-1", READY, "T-2", READY);
        AutoDispatch.DispatchPlan plan = policy.plan(tasks, readiness, cost, Set.of());
        assertEquals(List.of("T-1"), plan.launch());
        assertTrue(reasonOf(plan, "T-2"), reasonOf(plan, "T-2").contains("budget"));
    }

    @Test
    public void unknownSpendReadsAsZero() {
        // an actuals run without a cost segment contributes no spend
        AutoDispatch policy = AutoDispatch.of(10, 0.05, true);
        CostOverview cost = spent("fleet actuals: agent executor, model a/b");
        AutoDispatch.DispatchPlan plan = policy.plan(
                List.of(ticket("T-1")), Map.of("T-1", READY), cost, Set.of());
        assertEquals("the first (boundary) launch still fits",
                List.of("T-1"), plan.launch());
    }

    @Test
    public void budgetZeroMeansUnlimitedEvenWithRecordedSpend() {
        AutoDispatch policy = AutoDispatch.of(5, 0, true);
        CostOverview cost = spent("fleet actuals: cost 9.99 USD");
        List<Task> tasks = List.of(ticket("T-1"), ticket("T-2"), ticket("T-3"), ticket("T-4"), ticket("T-5"));
        Map<String, Readiness> readiness = Map.of(
                "T-1", READY, "T-2", READY, "T-3", READY, "T-4", READY, "T-5", READY);
        AutoDispatch.DispatchPlan plan = policy.plan(tasks, readiness, cost, Set.of());
        assertEquals(List.of("T-1", "T-2", "T-3", "T-4", "T-5"), plan.launch());
    }

    // ------------------------------------------------------------------
    // Candidate kinds / degenerate inputs / purity
    // ------------------------------------------------------------------

    @Test
    public void nonCandidateKindsWaitWithTheirReasons() {
        AutoDispatch policy = AutoDispatch.of(4, 0, true);
        List<Task> tasks = List.of(
                ticket("T-1"), ticket("T-2"), ticket("T-3"), ticket("T-4"), ticket("T-5"));
        Map<String, Readiness> readiness = Map.of(
                "T-1", READY, "T-2", WAITING, "T-3", BLOCKED, "T-4", RUNNING, "T-5", NOT_APPLICABLE);
        AutoDispatch.DispatchPlan plan = policy.plan(tasks, readiness, CostOverview.empty(), Set.of());
        assertEquals(List.of("T-1"), plan.launch());
        assertEquals(4, plan.skipped().size());
        assertTrue(reasonOf(plan, "T-2"), reasonOf(plan, "T-2").contains("WAIT_UPSTREAM"));
        assertTrue(reasonOf(plan, "T-3"), reasonOf(plan, "T-3").contains("BLOCKED"));
        assertTrue(reasonOf(plan, "T-4"), reasonOf(plan, "T-4").contains("RUNNING"));
        assertTrue(reasonOf(plan, "T-5"), reasonOf(plan, "T-5").contains("NOT_APPLICABLE"));
        for (AutoDispatch.Skip skip : plan.skipped()) {
            assertTrue(skip.id(), skip.reason() != null && !skip.reason().isBlank());
        }
    }

    @Test
    public void noVerdictMeansNoLaunch() {
        AutoDispatch policy = AutoDispatch.of(4, 0, true);
        AutoDispatch.DispatchPlan plan = policy.plan(
                List.of(ticket("T-1")), Map.of(), CostOverview.empty(), Set.of());
        assertEquals(List.of(), plan.launch());
        assertTrue(reasonOf(plan, "T-1"), reasonOf(plan, "T-1").contains("no readiness verdict"));
    }

    @Test
    public void nullAndEmptyInputsYieldAnEmptyPlan() {
        AutoDispatch policy = AutoDispatch.of(1, 1, true);
        assertEquals(new AutoDispatch.DispatchPlan(List.of(), List.of()),
                policy.plan(null, null, null, null));
        assertEquals(new AutoDispatch.DispatchPlan(List.of(), List.of()),
                policy.plan(List.of(), Map.of(), CostOverview.empty(), Set.of()));
    }

    @Test
    public void degenerateEntriesAreIgnored() {
        AutoDispatch policy = AutoDispatch.of(4, 0, true);
        AutoDispatch.DispatchPlan plan = policy.plan(
                Arrays.asList(null, ticket(null), ticket("T-1")),
                Map.of("T-1", READY), CostOverview.empty(), Set.of());
        assertEquals(List.of("T-1"), plan.launch());
        assertEquals(List.of(), plan.skipped());
    }

    @Test
    public void duplicateIdLaunchesOnceAndIsFlagged() {
        AutoDispatch policy = AutoDispatch.of(4, 0, true);
        AutoDispatch.DispatchPlan plan = policy.plan(
                List.of(ticket("T-1"), ticket("T-1"), ticket("T-2")),
                Map.of("T-1", READY, "T-2", READY), CostOverview.empty(), Set.of());
        assertEquals(List.of("T-1", "T-2"), plan.launch());
        assertTrue(reasonOf(plan, "T-1"), reasonOf(plan, "T-1").contains("duplicate"));
    }

    @Test
    public void sameInputsYieldTheSamePlanTwice() {
        AutoDispatch policy = AutoDispatch.of(2, 0.10, true);
        List<Task> tasks = List.of(ticket("T-3"), ticket("T-1"), ticket("T-9"), ticket("T-2"));
        Map<String, Readiness> readiness = Map.of(
                "T-1", READY, "T-2", READY, "T-3", READY, "T-9", STALE);
        CostOverview cost = spent("fleet actuals: cost 0.05 USD");
        Set<String> running = Set.of("T-8");
        assertEquals(policy.plan(tasks, readiness, cost, running),
                policy.plan(tasks, readiness, cost, running));
    }
}
