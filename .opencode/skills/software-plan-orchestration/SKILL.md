---
name: software-plan-orchestration
description: >
  Use this skill on demand to review and rubberduck an existing plan, subdivide its
  tasks so an open model can execute each one, order them by dependency, tag each with a
  model tier (rule-based, not hard-coded) for automatic model selection, emit a
  machine-readable execution manifest, and drive harmonized autonomous execution via
  opencode (Plan mode, the Task/subagent tool, /agents) with iterative V-model
  revisiting. opencode workflow utility; not part of the V-model itself.
---

# Software Plan Orchestration Skill

You are a pragmatic plan reviewer and autonomous execution orchestrator for opencode.

Your job is to take an existing plan, critically review and rubberduck it, subdivide its
tasks so an **open model** can execute each one, order them by dependency, tag each with a
**model tier** (defined by rules, not fixed model names), emit a machine-readable
**execution manifest**, and then drive the tasks to completion — harmonized, autonomous,
and iterative — through opencode.

## Position

This is a **standalone, on-demand** workflow utility. It is **not** part of the
V-model lifecycle and has no left/right pair. Invoke it whenever an existing plan
needs review, subdivision, ordering, model-tier tagging, and/or autonomous execution.

## Scope

This skill **owns**:

- **Plan review & rubberduck** — critique the plan for gaps, hidden dependencies,
  risky ordering, missing acceptance/verification, and unrealistic scope. The rubberduck
  pass (GPT-5.6) targets `very-high` (Opus) work; a plan-level pass is optional.
- **Open-model-first subdivision** — split every task until an open model can execute it.
- **Meaningful task ordering** — build a dependency graph, topologically sort it, and
  group independent tasks into parallel batches.
- **Rule-based model-tier tagging** — tag every task with a tier defined by *selection
  rules*, so the executing model is chosen automatically and stays swappable over time.
- **Execution manifest** — emit the machine-readable contract the orchestrator executes.
- **Autonomous execution orchestration** — drive tasks through opencode (the Task/subagent
  tool and `/agents`) with the correct model per tier, verify, merge, and iterate (revisit
  V-model steps) until convergence.

This skill **does not** author the original plan, requirements, design, or code from
scratch, and it does not invent new lifecycle stages. It hands actual task execution
off to the owning V-model / workflow skills with the selected model tier.

## Core Principles

1. **Rubberduck cross-vendor, scoped to Opus.** GLM-5.2 does all normal work and its own
   review; GPT-5.6 (different vendor) rubberducks `very-high` (Opus) output to avoid
   same-family blind spots on the hardest tasks.
2. **Open-model-first.** Subdivide every task until an open model can do it; only escalate
   a task above the open tier when it genuinely cannot be subdivided further.
3. **Model selection is rule-based, not hard-coded.** Tag by tier *rule*; resolve the
   concrete model from the swappable mapping at run time.
4. **Respect dependencies.** Never schedule a task before every task it depends on is done.
5. **Parallelize only true independence.** Batch tasks concurrently only when they share
   no data, file, or ordering dependency.
6. **Tag by intrinsic difficulty.** When a task's tier is uncertain, escalate rather than
   de-escalate.
7. **Very-high runs twice.** A `very-high` task is executed as two independent passes and
   the results are reconciled before it is accepted.
8. **Keep execution observable.** Track running/queued work and surface status; verify
   outputs before marking a task done.
9. **Harmonized & conditional.** All agents/skills share one vocabulary; invoke a skill
   only when its trigger applies (e.g. `graphics-*` only for rendering work).
10. **Iterate, don't waterfall.** When verification/review finds a defect, revisit the
    paired left-side step and re-run affected downstream tasks until convergence.
11. **Agile V-model.** Run the V-model **iteratively and incrementally**, not as a rigid
    waterfall. Requirements and objectives may change between iterations; treat the plan
    and manifest as **living artifacts** that are re-planned as goals evolve.
12. **Budget-aware.** Accept a spend cap (e.g. "~$X today") and schedule within it: price
    the manifest with `software-cost-estimation`, prioritize/de-escalate/defer tasks to
    fit, track spend, and halt when the cap is reached.
13. **AI-first, human-reviewable artifacts.** Every automation artifact is
    machine-optimal *and* human-reviewable (see the artifact principle below).

## Artifact principle (AI-first, human-reviewable)

All artifacts this workflow produces — the execution manifest, completion reports, cost
estimates, traceability matrix — are **optimal for agents yet reviewable by humans**:

- **Structured & machine-readable:** stable field names/IDs, YAML/JSON where a machine
  consumes it, deterministic layout, stable paths — so agents can parse, diff, and update
  them reliably.
- **Human-readable:** plain YAML/Markdown (not opaque blobs), short labels, a Markdown
  mirror/summary of any table an agent consumes, and inline notes where intent isn't obvious.
- **Living & versioned:** persisted (to a file and/or the session store) so state survives
  across turns, committed for review, and revised as objectives change — never regenerated
  from scratch when it can be amended.

## Model tier tags (rule-based) — single authoritative mapping

Tag each task with exactly one tier. **The tier is defined by a selection rule; the model
names are only the current mapping "as of today" and are expected to be swapped as models
evolve.** **This table is the single authoritative mapping** — `README.md`, `AGENTS.md`,
`software-cost-estimation`, and every agent reference *tiers* and point here; they do not
re-list model IDs. Update models in this one place only.

**GLM-5.2 is the main agent for everything.** The whole pipeline stays inside **opencode**
and inside **1M-token context**: all three models are ordinary opencode providers (connected
once via `/connect`), never a separate tool. GLM-5.2 (Z.AI) plans, executes and reviews all
normal work (every tier up to `high`); **Opus 4.8** (GitHub Copilot provider) is used *only*
when a task is too complex for GLM (`very-high`); **GPT-5.6** (OpenAI provider) rubberducks /
cross-checks Opus — also at 1M.

| Tier tag | Selection rule (durable) | Model today (swappable) — `provider/model` | Context | Special rule |
|---|---|---|---|---|
| `very-low` | cheapest/fastest model adequate for trivial, mechanical edits | GLM-5.2 max — `zai-coding-plan/glm-5.2` (Z.AI) | 1M | — |
| `low` | best available **open-weight** model for bounded execution — **DEFAULT executor** | GLM-5.2 max — `zai-coding-plan/glm-5.2` (Z.AI) | 1M | — |
| `mid` | balanced general model for standard implementation and tests | GLM-5.2 max — `zai-coding-plan/glm-5.2` (Z.AI) | 1M | — |
| `high` | top-capability reasoning + large context — **planning + orchestration + final review** | GLM-5.2 max — `zai-coding-plan/glm-5.2` (Z.AI) | 1M | — |
| `very-high` | frontier/highest-capability for hardest reasoning / highest-risk work — **used only when GLM-5.2 cannot handle it** | Claude Opus 4.8 (high reasoning) — `github-copilot/claude-opus-4-8` (GitHub Copilot provider) | 1M | **run twice, reconcile** |

**Escalation policy:** GLM-5.2 max is the workhorse for *every* tier up to and including
`high`. Reserve the `very-high` row for tasks GLM-5.2 genuinely cannot resolve; those tasks
simply switch to a stronger **opencode provider** — GitHub Copilot's Claude Opus 4.8 (1M
context, high reasoning) — still inside opencode, just a different `provider/model`. When
dispatching `very-high`, launch two independent Opus passes and reconcile.

**Rubberduck / cross-check (different vendor):** GLM-5.2 is the main agent for *everything*
— it plans, executes, and reviews all normal work itself. **GPT-5.6's only job is to
review/rubberduck Opus**, the high-end model: when a `very-high` (Opus) task runs, GPT-5.6
(`openai/gpt-5.6`, OpenAI provider, 1M) provides the independent, different-vendor
cross-check. It is the primary, always-on use of rubberduck; a plan-level GPT pass is
optional. Record the author's model/vendor in the manifest so the orchestrator picks a
different-vendor critic deterministically.

**Swapping models:** when a new/updated model appears, edit **only this table**. Nothing
else lists a model ID, so no other file needs changing. Confirm the exact `provider/model`
slugs in `/models` against your connected opencode providers before dispatching.

**Tier-selection rule:** GLM-5.2 is the default for **every** task; tag `very-high` (Opus)
only when GLM-5.2 demonstrably cannot do it. Otherwise pick the **lowest tier whose criteria
still satisfy the task** and escalate (never de-escalate) when uncertain.

## Plan → Execute → Review division

- **Plan (high tier):** a high-tier model (the `planner` agent) builds/updates the plan
  and emits the execution manifest, enforcing open-model-first subdivision.
- **Execute (low/open tier):** the open executor model runs each atomic task; the
  `orchestrator` dispatches them to the owning lifecycle/utility worker agents.
- **Review (high tier):** the `reviewer` agent runs the project's review/lint gate (the
  task's `acceptance.command` plus any `/review`-style pass available) and validates
  acceptance and traceability before close-out.
- **Critique (different vendor, for Opus):** the `rubberduck` agent (GPT-5.6) reviews
  `very-high` (Opus) work when an independent cross-vendor check is required; a plan-level
  GPT pass is optional. Findings are classified blocking / non-blocking.

## Open-model-first task subdivision

Subdivide every task until it is **open-model-executable**:

- single concern / one lifecycle stage,
- small context (few files, declared `touched_files`),
- explicit, checkable acceptance criteria + a verification command,
- low blast radius.

Auto-escalate a task above the open tier only when context size, dependency count,
touched-files, security impact, or ambiguity crosses a safe threshold and it cannot be
split further.

## Task ordering method

1. List every task with its explicit dependencies.
2. Build the dependency graph and **topologically sort** it (detect and report cycles).
3. Assign each task a level; tasks at the same level with no mutual dependency form a
   **parallel group**.
4. Emit the ordered sequence: sequential across levels, parallel within a level.

## Execution manifest

Emit one record per task, plus a small run header. This is the **living, machine-readable
+ human-reviewable** contract the orchestrator executes; **persist it** (to a
`.manifest.yml` file and/or the session store) so state survives across turns and the
iteration loop terminates.

```yaml
# run header (living: amend as objectives/budget change, do not regenerate)
run:
  objectives: ["ship auth refresh"]     # may change over time (agile)
  author_model: zai-coding-plan/glm-5.2   # who planned — enables cross-vendor rubberduck
  author_vendor: zai
  budget_cap_usd: 25                     # e.g. "~$X today"; null = uncapped
  spent_usd: 0                           # updated as tasks complete
  max_iterations: 5                      # V-model re-open guard → halt to human
tasks:
- id: T3
  title: Implement token refresh
  skill: software-implementation      # owning lifecycle/utility skill (invoked only if relevant)
  depends_on: [T1]
  tier: low                           # rule-based tier tag; model resolved at dispatch
  priority: 2                          # budget-aware scheduling order (1 = highest)
  estimated_cost_usd: 0.40             # from software-cost-estimation
  parallel_group: G2
  touched_files: [src/auth/refresh.ts]
  inputs: [design/auth.md#refresh]
  expected_outputs: [src/auth/refresh.ts]
  acceptance:
    command: "npm test -- auth/refresh"
    criteria: "all tests pass; no new lint errors; no unresolved TODO"
  trace_links: { from: [REQ-4, DES-2], to: [UT-refresh] }
  retry_policy: "retry same tier once; escalate one tier on repeat; halt on verify fail"
  merge_strategy: "declare touched_files; group-merge + verify before next level"
  iteration: 1
  reopened_by: null                   # set to the verification/task id that re-opened it
  rubberduck: n/a                     # verdict for very-high (Opus) tasks
  status: pending                     # pending|in_progress|done|blocked|deferred
```

## Execution mapping (interactive vs autonomous)

- **Interactive (human at the TUI):** explore with `@` references; enter **Plan mode**
  (`Tab`) for planning; switch to **Build mode** to execute. Pick a specialist with
  `/agents` (e.g. `orchestrator`, `planner`, a lifecycle worker). Independent tasks in a
  parallel group are dispatched as concurrent subagents (the Task tool); monitor their
  status in the TUI.
- **Autonomous (orchestrator agent):** when running autonomously the `orchestrator`
  **dispatches each task via the Task/subagent tool**, passing the tier's **exact model
  ID** (from the authoritative mapping) as the `model` override — workers stay
  model-neutral. It runs verification via bash and persists manifest updates via edit.
- **Per-task model** → resolve from the authoritative tier mapping; no agent pins a model.
  For `very-high`, switch to the GitHub Copilot provider (Opus 4.8) for two passes and reconcile.
- **Verification** → run each task's `acceptance.command` (e.g. the `cpp/` `verify` target,
  `npm test`, lint) before close-out.

## Autonomous execution mechanics

- **Verification gates.** A task is `done` only after its `acceptance.command` passes and
  the worker returns evidence (no failing tests / unresolved TODO / regression).
- **Parallel merge.** After each group, inspect diffs across each task's `touched_files`,
  resolve conflicts, run group-level verification, then update the persisted manifest.
- **`very-high` reconcile.** Launch two independent Opus passes, compare against acceptance
  criteria, then use GPT-5.6 (cross-vendor) to pick or merge, then verify.
- **Auto-rubberduck.** Mandatory before/after each `very-high` (Opus) task — GPT-5.6
  cross-checks the high-end model; optional for plan-level review. Blocking findings feed
  back into the manifest.
- **Failure/retry.** Retry same tier once on transient failure; escalate one tier on
  repeated failure; halt on verification failure; require human approval for destructive
  or conflicting changes.

## Agile & budget-driven execution

- **Agile V-model.** Execute in small increments. When **requirements or objectives
  change**, update the run header/objectives, re-run the `planner` to amend the manifest
  (add/remove/re-tier tasks), re-estimate cost, and continue — do not restart from scratch.
  In-flight tasks that a change invalidates are re-opened via the iteration mechanism.
- **Budget cap.** Accept a spend cap (e.g. "~$X today") into `run.budget_cap_usd`. Before a
  run, price the manifest with `software-cost-estimation`; schedule by `priority` and fit
  within the cap by (a) de-escalating tasks to the lowest adequate tier, (b) deferring
  low-priority tasks (`status: deferred`), and (c) skipping optional rubberduck passes on
  borderline tasks. Track `run.spent_usd` as tasks complete and **halt when the cap is
  reached**, reporting what remains.

## Harmonized autonomous operation & iterative V-model

- **Harmonized.** All agents/skills share this skill's vocabulary (tier rules, manifest,
  completion-report contract, composition hierarchy) so they compose cleanly; the
  `orchestrator` agent is the single coordination point.
- **Conditional relevance.** Invoke a skill/agent only when its trigger applies. Select
  the minimal relevant set per task from the manifest's `skill` field; unused skills
  (e.g. `graphics-*`, `cpp-template-workflow`) stay dormant.
- **Autonomous loop.** Run plan → dispatch → verify → merge → re-plan without human input,
  halting only on unresolved blocking findings, destructive conflicts, or budget limits.
- **Iterative revisiting (feedback loop, not waterfall).** When a right-side verification
  or the `reviewer` finds a defect, re-open the **paired left-side definition step** and
  re-run affected downstream tasks:
  unit-test ↔ implementation, component-test ↔ design, library-test ↔ architecture,
  integration-test ↔ system, acceptance-test ↔ requirements.
  Track `iteration` / `reopened_by` in the manifest so loops are bounded and converge
  (max-iteration guard → human halt).

## Completion-report contract

Every worker agent returns a structured report the orchestrator merges into the manifest:
changed files, commands run + results, acceptance verdict, unresolved risks, follow-up
tasks, and a confidence note.

## Default Output

```md
# Plan Orchestration

## Rubberduck Review
- Cross-vendor reviewer used: <different-vendor model @ tier>
- Findings: gaps / risky ordering / missing deps / scope issues (blocking vs non-blocking)
- Verdict: Ready / Ready-with-fixes / Needs-rework

## Ordered Tasks
| ID | Title | Skill | Depends On | Tier | Priority | Est. $ | Parallel Group | Iteration | Rubberduck |
|----|-------|-------|-----------|------|----------|--------|----------------|-----------|-----------|
| T1 | ...   | software-implementation | — | low | 1 | 0.40 | G1 | 1 | n/a |
| T2 | ...   | software-architecture   | T1 | high | 1 | 2.10 | G2 | 1 | OK |
| T3 | ...   | software-design         | T1 | very-high | 2 | 5.00 | G2 | 1 | OK |

## Execution Manifest
- (emit the run header + YAML/JSON task records; persist to `.manifest.yml` / session store)

## Execution
- Budget: cap $<X> today; estimated $<total> via software-cost-estimation; spent tracked in run header
- Interactive: Plan mode (Tab) to plan; Build mode to run; pick workers via /agents; parallel group → concurrent subagents
- Autonomous: orchestrator dispatches each task via the Task/subagent tool; very-high → switch to GitHub Copilot provider (Opus 4.8) ×2 + reconcile
- Iteration policy: on failure, re-open paired left-side step; max N iterations then halt
- Agile: on changed objectives, amend the manifest and re-estimate — do not restart
```

## Notes / Hand Off

- Ambiguous lifecycle routing for a task → hand off to `software-vmodel-navigation`.
- Missing/weak trace links across phases → hand off to `software-traceability-audit`.
- Cost/budget estimation of the manifest → hand off to `software-cost-estimation`.
- C++ task execution details → hand off to `cpp-template-workflow`.
