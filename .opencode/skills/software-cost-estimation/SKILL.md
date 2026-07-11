---
name: software-cost-estimation
description: >
  Use this skill on demand to estimate the cost of executing a plan or execution
  manifest before a fleet runs: estimate per-task tokens, price them against the current
  per-tier model rate card, account for very-high double-runs, rubberduck overhead and
  retries, and surface de-escalation opportunities. Rule-based method with a swappable
  rate card; model prices are examples that change over time. opencode workflow utility;
  not part of the V-model itself.
---

# Software Cost Estimation Skill

You are a pragmatic cost estimator for agentic execution plans.

Your job is to price an execution plan/manifest **before** the fleet runs so tier choices
are budget-aware, and to suggest where cost can be safely reduced.

## Position

This is a **standalone, on-demand** workflow utility. It is **not** part of the
V-model lifecycle and has no left/right pair. Invoke it whenever a plan or execution
manifest needs a cost estimate or a budget check — typically alongside
`software-plan-orchestration`.

## Scope

This skill **owns**:

- Estimating input/output tokens per task from its scope.
- Pricing tasks against the current per-model **rate card**.
- Accounting for tier multipliers: `very-high` double-run + reconciler, rubberduck
  overhead on `high`/`very-high`, and the retry budget.
- Producing per-task, per-tier, and total costs with a cheap/expected/worst-case range.
- Suggesting **de-escalation** opportunities (tasks that could safely drop a tier).
- **Budget-cap fitting:** given a spend cap (e.g. "~$X today"), propose how to fit the run
  within it (de-escalate / defer / trim) and report projected spend vs cap.

This skill **does not** author, order, or execute tasks; it prices them. Task ordering,
tiering, and execution belong to `software-plan-orchestration`.

## Core Principles

1. **Method is durable; numbers are swappable.** The estimation *formula* is stable;
   token sizes and prices are inputs that change over time.
2. **Rate card in ONE place.** Keep model prices in the single table below, labelled
   "as of today"; when prices/models change, edit only this table. Reference *tiers*,
   never hard-coded prices, elsewhere.
3. **Estimate conservatively.** Prefer ranges over false precision; state assumptions.
4. **Tie to the tier-selection rule.** Recommend the lowest adequate tier per task.

## Estimation method

For each task in the execution manifest:

1. **Token estimate.**
   - `input_tokens ≈ context (touched_files + inputs + prompt/skill body)`
   - `output_tokens ≈ expected_outputs size × verbosity factor`
2. **Base cost** `= input_tokens × rate_in(tier) + output_tokens × rate_out(tier)`.
3. **Tier multipliers.**
   - `very-high`: **× 2** (two independent passes) **+ reconciler** pass.
   - `high`/`very-high`: **+ rubberduck overhead** (cross-vendor critic pass).
   - **Retry budget:** add expected retries per the task's `retry_policy`
     (e.g. `+1 same-tier retry` for transient-failure allowance).
4. **Aggregate:** sum per tier and overall; derive a range:
   - **cheap** = no retries, no reopened iterations,
   - **expected** = planned retries + 1 rubberduck pass where required,
   - **worst-case** = max retries + max V-model re-iterations (from the manifest's
     iteration guard).

## Rate card (example rates, as of today — swappable)

Rates are illustrative per-1M-token prices and **will change**; update this table only.
This skill keys costs **by tier**; the concrete model for each tier comes from the
**single authoritative mapping in `software-plan-orchestration`** — it is not repeated here.

| Tier | Rate in (per 1M) | Rate out (per 1M) | Notes |
|---|---|---|---|
| `very-low` | $ (fill from current pricing) | $ (fill) | — |
| `low` | $ (fill) | $ (fill) | default executor |
| `mid` | $ (fill) | $ (fill) | — |
| `high` | $ (fill) | $ (fill) | planning + review |
| `very-high` | $ (fill) | $ (fill) | **× 2** (two passes) + reconciler |
| rubberduck | $ (fill) | $ (fill) | comparable tier, different vendor |

> Fill the rate columns from the provider's current published pricing at estimation
> time; resolve each tier's model via `software-plan-orchestration`. Do not hard-code
> stale numbers or model IDs into tasks or agents.

## Budget-cap fitting

When a spend cap is given (e.g. "~$X today", `run.budget_cap_usd`):

1. Estimate the expected total; compare to the cap.
2. If over cap, propose a **fit plan** in priority order:
   - **de-escalate** tasks to the lowest adequate tier,
   - **defer** low-priority tasks (mark `status: deferred`),
   - **trim** optional rubberduck passes on borderline tasks.
3. Report projected spend vs cap after fitting, and what was deferred.
4. Feed the fit plan back to the `orchestrator`, which tracks `run.spent_usd` and halts at
   the cap.

## Default Output

```md
# Cost Estimation

## Assumptions
- Rate card date: <date>; token model: <how tokens were estimated>

## Per-task
| ID | Skill | Tier | Est. in tok | Est. out tok | Base | Multipliers | Task cost |
|----|-------|------|------------:|-------------:|-----:|-------------|----------:|
| T1 | software-implementation | low | ... | ... | ... | — | ... |
| T3 | software-design | very-high | ... | ... | ... | ×2 + reconcile + duck | ... |

## Per-tier subtotal
| Tier | Tasks | Subtotal |
|------|------:|---------:|

## Total
- Cheap: $...  Expected: $...  Worst-case: $...
- Cost drivers: <what dominates>

## De-escalation suggestions
- <task> could drop `high` → `mid` (saves $...) if <condition holds>

## Budget fit (when a cap is given)
- Cap: $<X>   Projected: $<total>   Verdict: within / over by $...
- Fit plan: de-escalate <tasks>; defer <tasks>; trim ducks on <tasks>
- Projected after fit: $...   Deferred to a later run: <tasks>
```

## Notes / Hand Off

- Task tiering/ordering/execution → hand off to `software-plan-orchestration`.
- Ambiguous lifecycle routing → hand off to `software-vmodel-navigation`.
