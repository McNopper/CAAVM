# AGENTS.md — Hephaestus Copilot workflow

Repository-level conventions for agentic work in this repository.

## Lifecycle routing (V-model)

Use these skills as the default path:

```text
Software Requirements    (01) ↔ Acceptance Test       (10)
Software System          (02) ↔ Integration Test      (09)
Software Architecture    (03) ↔ Library Test          (08)
Software Design          (04) ↔ Component Test        (07)
Software Implementation  (05) ↔ Unit Test             (06)
```

If a request is ambiguous, route with `software-vmodel-navigation` first.

## Canonical composition hierarchy

The dividing line is **reuse scope** (static-vs-shared linkage is a build decision):

- **Unit** → smallest implementation element with a clear interface.
- **Component** → composed of units; **internal** to this software (linked in).
- **Library** → composed of components; **independently deployable and reusable
  outside this software**; exposes a clear interface and dependency rules.
- **Software system** → composed of libraries plus external/system interfaces.
- **Package/folder** (and language *modules*) → organization only; not a lifecycle level.

## C++ template routing

For C++ tasks in this repository, default to the `cpp/` template workflow:

1. Use `cpp-template-workflow`.
2. Run commands from `cpp/`.
3. Use canonical targets from `cpp/AGENTS.md` (`verify`, `verify-full`, `format`, `tidy`, `cppcheck*`, `docs`).
4. Treat "analysis skipped" on unsupported toolchains as degraded signal, not failure.

## Traceability expectations

- Keep explicit links between each left-side skill artifact and its right-side verification artifact.
- Use `software-traceability-audit` when trace links are missing or unclear.

## Plan review, ordering & auto-execution

For reviewing an existing plan, ordering its tasks, and driving them to completion:

1. Use `software-plan-orchestration`.
2. It rubberducks the plan with a **different-vendor** model at a comparable tier.
3. It **subdivides every task so an open model can execute it** (open-model-first).
4. It orders tasks by dependency (topological sort), groups independent tasks for
   parallel execution, and emits a machine-readable **execution manifest**.
5. It tags each task with a **rule-based model tier**. The tier is defined by a
   *selection rule*; concrete model IDs are only an example mapping **as of today**,
   expected to be swapped as models evolve. **Models are never hard-coded** — agents/docs
   reference *tiers*, and the **single authoritative tier→model mapping lives in
   `software-plan-orchestration`** (`.github/skills/software-plan-orchestration/SKILL.md`).
   Update models there and nowhere else.

   | Tier | Selection rule (durable) |
   |---|---|
   | `very-low` | cheapest/fastest for trivial, mechanical edits |
   | `low` | best available **open-weight** model — **default executor** |
   | `mid` | balanced general model for standard impl/tests |
   | `high` | top-capability reasoning + large context — **planning + review** |
   | `very-high` | frontier/highest-risk — **run twice & reconcile** |

   Tier-selection rule: pick the **lowest tier whose criteria still satisfy the task**;
   escalate (never de-escalate) when uncertain. See the authoritative mapping for the
   example model IDs to use as the per-task model override.
6. **Plan → Execute → Review:** a **high**-tier model plans; the **open** executor runs
   each subdivided task; a **high**-tier model reviews at the end.
7. It drives execution **iteratively (agile V-model)**: interactively via `/autopilot`,
   `/fleet`, `/tasks`, `/review`; autonomously the `orchestrator` dispatches each task via
   the **subagent tool** with the tier's exact model ID. On a failed test/review it
   re-opens the paired left-side step and re-runs downstream until convergence. When
   **requirements/objectives change**, it amends the living manifest and re-estimates
   rather than restarting.
8. **Budget-driven:** accept a spend cap (e.g. "~$X today"); price the manifest with
   `software-cost-estimation`, fit within the cap (de-escalate / defer / trim), track
   spend, and halt at the cap.

**AI-first, human-reviewable artifacts:** the manifest, completion reports, cost estimates
and traceability matrix are structured/machine-parseable **and** human-readable
(plain YAML/Markdown, stable IDs), persisted and versioned as living artifacts.

## Custom agents (`.github/agents/`)

The workflow is operationalized by **23 custom agents** in `.github/agents/*.agent.md`,
invoked with `/agent` (or referenced in a prompt). Agents are **model-neutral** — they
reference a **tier**, and the concrete model is resolved from the authoritative mapping in
`software-plan-orchestration` at dispatch. This keeps model choice dynamic and centralized.

- **Role agents (5):** `orchestrator` (coordination; dispatches via the subagent tool,
  persists the manifest, runs verification, enforces the budget cap), `planner` (high-tier
  planning + living manifest), `executor` (open-tier task execution), `reviewer`
  (high-tier final review), `rubberduck` (cross-vendor critic).
- **V-model lifecycle agents (10):** one per skill (`software-requirements` …
  `software-acceptance-test`) — thin wrappers that delegate to their `SKILL.md`. (They are
  convenience wrappers; the orchestrator may equivalently dispatch a worker with a
  "use the `<skill>` skill" instruction.)
- **On-demand utility agents (8):** `software-vmodel-navigation`,
  `software-traceability-audit`, `software-plan-orchestration`, `software-cost-estimation`,
  `cpp-template-workflow`, `graphics-window-screenshot`, `graphics-renderdoc-profiling`,
  `graphics-render-comparison`.

**Harmonized & conditional:** all agents share one vocabulary (tier rules, execution
manifest, completion-report contract, composition hierarchy). The `orchestrator` invokes a
skill/agent **only when its trigger applies** — `graphics-*` and `cpp-template-workflow`
stay dormant unless a task needs them — so the system runs autonomously with the minimal
relevant set.


## Copilot CLI feature usage (recommended)

- Use `/plan` for multi-file or multi-phase changes before implementation.
- Use `/agent` to select a role/lifecycle/utility custom agent from `.github/agents/`.
- Use `/autopilot` and `/fleet` for parallelizable work across many skill/docs files.
- Use `/tasks` to monitor delegated/background work.
- Use `/skills` and `/env` to validate that repository skills and instructions are loaded.
- Use `/review` and `/security-review` before merge when code changes are involved.
- Use `/research` when terminology or process decisions require external references.
