package com.opencode.ide.board.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opencode.ide.tasks.StageReadiness;
import com.opencode.ide.tasks.StageReadiness.Kind;
import com.opencode.ide.tasks.StageReadiness.Readiness;
import com.opencode.ide.tasks.Task;

/**
 * The SWT-free auto-dispatch policy (ROADMAP H6 piece 2): turns one
 * snapshot's {@link StageReadiness} verdicts plus the fleet
 * {@link CostOverview} into a concrete launch list — which tickets may start
 * now, and why every other one waits. A pure value object over its inputs:
 * no I/O, no clock, never throws (null and degenerate inputs are tolerated).
 *
 * <p>Selection: candidates are the READY tickets plus — only when
 * {@code includeStale} — the STALE ones (re-runs made necessary by a changed
 * upstream). In the launch order STALE always precedes READY (rework before
 * new work), and within both groups ids sort in natural order, so the same
 * inputs always yield the same plan. The launch list is capped at
 * {@code maxConcurrent - alreadyRunning.size()} (floor 0), and a cost budget
 * stops admitting once the running total plus {@link #ESTIMATED_COST_USD}
 * per further launch would EXCEED {@code costBudgetUsd} — landing exactly on
 * the budget is still admitted.</p>
 *
 * <p>Budget basis (documented choice): the handed-in overview's
 * <b>grand total</b> ({@link CostOverview#project()}), because {@code plan}
 * has no sprint scope of its own — the Board passes its project-wide
 * overview, so the cap guards the whole project's recorded spend. Unknown
 * spend (no cost in any actuals comment) reads as 0: what is not recorded
 * cannot block dispatch. {@code costBudgetUsd == 0} means unlimited;
 * negative budgets (and {@code maxConcurrent < 1}) are rejected at
 * construction.</p>
 */
public record AutoDispatch(int maxConcurrent, double costBudgetUsd, boolean includeStale) {

    /**
     * Flat per-launch cost placeholder until the cost loop calibrates a real
     * estimate: every admitted launch books this against the budget.
     */
    public static final double ESTIMATED_COST_USD = 0.05;

    public AutoDispatch {
        if (maxConcurrent < 1) {
            throw new IllegalArgumentException("maxConcurrent must be >= 1: " + maxConcurrent);
        }
        if (!(costBudgetUsd >= 0)) {
            throw new IllegalArgumentException(
                    "costBudgetUsd must be >= 0 (0 = unlimited): " + costBudgetUsd);
        }
    }

    /** Validates (see class doc) and creates the policy. */
    public static AutoDispatch of(int maxConcurrent, double costBudgetUsd, boolean includeStale) {
        return new AutoDispatch(maxConcurrent, costBudgetUsd, includeStale);
    }

    /**
     * Plans one dispatch, never throws.
     *
     * @param tasks          the dispatchable pool (e.g. the current sprint's tickets);
     *                       null elements and tickets without id are skipped
     * @param readiness      the verdicts to gate on (typically
     *                       {@link StageReadiness#evaluate(List)} over the whole project);
     *                       ids without a verdict never launch
     * @param cost           the actuals overview whose grand total the budget counts;
     *                       {@code null} reads as no recorded spend
     * @param alreadyRunning ids with a live fleet job — never launched again,
     *                       and counted against {@code maxConcurrent}
     */
    public DispatchPlan plan(List<Task> tasks, Map<String, Readiness> readiness,
            CostOverview cost, Set<String> alreadyRunning) {
        Map<String, Readiness> verdicts = readiness == null ? Map.of() : readiness;
        Set<String> running = alreadyRunning == null ? Set.of() : alreadyRunning;
        double spend = spentSoFar(cost);

        List<String> stale = new ArrayList<>();
        List<String> ready = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (tasks != null) {
            for (Task t : tasks) {
                if (t == null || t.id == null || !seen.add(t.id)) {
                    continue;
                }
                if (running.contains(t.id)) {
                    continue;
                }
                Readiness r = verdicts.get(t.id);
                if (r == null) {
                    continue;
                }
                if (r.kind() == Kind.STALE) {
                    if (includeStale) {
                        stale.add(t.id);
                    }
                } else if (r.kind() == Kind.READY) {
                    ready.add(t.id);
                }
            }
        }
        stale.sort(Comparator.naturalOrder());
        ready.sort(Comparator.naturalOrder());

        int capacity = Math.max(0, maxConcurrent - running.size());
        Set<String> launchIds = new LinkedHashSet<>();
        Map<String, String> withheld = new LinkedHashMap<>();
        List<String> ordered = new ArrayList<>(stale);
        ordered.addAll(ready);
        for (String id : ordered) {
            if (launchIds.size() >= capacity) {
                withheld.put(id, "concurrency cap reached (max " + maxConcurrent
                        + ", " + running.size() + " already running)");
            } else if (costBudgetUsd > 0 && spend + ESTIMATED_COST_USD > costBudgetUsd) {
                withheld.put(id, "cost budget " + CostOverview.usd(costBudgetUsd) + " reached (spent "
                        + CostOverview.usd(spend) + ", estimated "
                        + CostOverview.usd(ESTIMATED_COST_USD) + " per launch)");
            } else {
                launchIds.add(id);
                spend += ESTIMATED_COST_USD;
            }
        }

        List<Skip> skipped = new ArrayList<>();
        if (tasks != null) {
            Set<String> processed = new HashSet<>();
            for (Task t : tasks) {
                if (t == null || t.id == null) {
                    continue;
                }
                if (!processed.add(t.id)) {
                    skipped.add(new Skip(t.id, "duplicate id in the input"));
                } else if (!launchIds.contains(t.id)) {
                    skipped.add(new Skip(t.id, waitingReason(t.id, verdicts, running, withheld)));
                }
            }
        }
        return new DispatchPlan(List.copyOf(launchIds), List.copyOf(skipped));
    }

    /** One dispatch decision: the launchable ids in launch order, plus every other input id with why it waits. */
    public record DispatchPlan(List<String> launch, List<Skip> skipped) {

        public DispatchPlan {
            launch = List.copyOf(launch);
            skipped = List.copyOf(skipped);
        }
    }

    /** A withheld ticket id plus the human-readable reason it was not launched. */
    public record Skip(String id, String reason) {
    }

    private String waitingReason(String id, Map<String, Readiness> verdicts,
            Set<String> running, Map<String, String> withheld) {
        if (running.contains(id)) {
            return "already running (counts against the concurrency cap)";
        }
        String withheldReason = withheld.get(id);
        if (withheldReason != null) {
            return withheldReason;
        }
        Readiness r = verdicts.get(id);
        if (r == null) {
            return "no readiness verdict";
        }
        if (r.kind() == Kind.STALE && !includeStale) {
            return "STALE (re-run needed) excluded: includeStale is off";
        }
        return r.kind() + ": " + r.reason();
    }

    /** The overview's grand total; unknown ({@code null}) spend reads as 0. */
    private static double spentSoFar(CostOverview cost) {
        if (cost == null || cost.project() == null || cost.project().costUsd() == null) {
            return 0;
        }
        return cost.project().costUsd();
    }
}
