# Hephaestus — Portable Process Specification

> Tool-agnostic, step-by-step procedure for running **one increment** of the
> Cyclic Agentic Agile V-Model. Follow this by hand, with any agent harness, or
> as the contract the automated workflow implements. Every `${config.x}` refers
> to [`../config/hephaestus.config.yaml`](../config/hephaestus.config.yaml).
>
> Repeat this whole procedure once per entry in `${config.project.backlog}`.
>
> **Hybrid construction — walking skeleton first (`${config.strategy}`, `${config.hybrid}`).**
> The left arm decomposes top-down for *intent*, but its contracts are **provisional**; each
> increment then builds a **walking skeleton** — the thinnest end-to-end vertical **slice** of
> *real* units (bottom-up) that exercises the riskiest assumptions — before adding breadth. Top-down
> and bottom-up **meet in the middle**, and an **Adaptation** step (controlled feedback up) promotes
> validated contracts `provisional → stable` and revises the disproven ones. Maturity is tracked
> **per slice** (`${config.strategy.maturity_scope}`): each validated slice climbs its OWN ladder
> (`${config.strategy.maturity_levels}` — `mvp → harden → complete`), and one run **advances every
> slice that is ready**. The first touch of a slice builds its walking skeleton; each later run reads
> `OUTPUT.md` and **deepens** ready slices until every idea in `INPUT.md` is resolved at the top rung.
> Each rung may relax the quality gates; the **effective gates = `${config.quality_gates}` with that
> rung's overrides merged on top**.
>
> **Commit per phase, on the working branch (`${config.git}`).** Every phase persists its
> content and commits it onto the current branch as it finishes, so progress lands on
> `main` incrementally and the loop is resumable. The right arm is a bottom-up **tree of gated
> merges**: each node is built on its own branch and merged into its parent only once its test is green.
> And **every phase writes a small trace file** (`${config.living_artifacts.phase_trace}`)
> **regardless of the documentation toggle** — a file-level trail, and it keeps each commit
> non-empty.
>
> **Carry-forward:** every stage writes a *persisted* artifact (`${config.living_artifacts}`).
> Start each increment by loading the prior ADRs (with their `provisional`/`stable` state), the
> traceability matrix, the debt log, and the **assumption-debt ledger** — they are the shared memory
> of the loop for **both human and agent**, last cycle's decisions/debt are this cycle's constraints
> (`${config.carry_forward}`), and open assumption debt is preferentially retired before new breadth.
>
> **Documentation is minimal & effective** (`${config.references.documentation.principle}`):
> capture decisions, interfaces and rationale — not the obvious; a diagram beats prose;
> mark irrelevant sections _n/a_. The **`${config.toggles.documentation}`** toggle scales
> this: `full` (arc42 + API docs + UML), `minimal` (ADRs + sketch only), or `off` (code
> and tests are the spec — skip the Architecture (Stage 3) arc42 docs and drop the doc item from the DoD).

---

## Interface: `INPUT.md` → process → `OUTPUT.md`

The human drives the whole loop through two markdown files at the project root
(`${config.interface.input}` / `${config.interface.output}`). **Strict ownership:**
`INPUT.md` is **human-only** (the process only ever *reads* it, never writes it);
`OUTPUT.md` is **process-only** (the human never edits it). Everything the process
creates, derives, or updates is recorded in `OUTPUT.md`.

- **`INPUT.md` (human writes, anytime — even mid-run).** Plain sentences: the language,
  design style, tools, gates, docs level, and feature ideas. It can be **super minimal** —
  the config already holds defaults, so you write only what should differ. A single
  sentence is a valid input.
- **Stage 0 — Intake (process).** *Read* `INPUT.md` **and `OUTPUT.md`** (the latter carries prior
  loops' **per-slice** state) and **rewrite the project files to match**: map high-level choices into
  `config/hephaestus.config.yaml` (language, toolchain, quality_gates, toggles.documentation,
  models, …) and turn feature ideas into backlog increments. Also load the prior ADRs (with their
  `provisional`/`stable` state), the traceability matrix, the debt log, and the **assumption-debt
  ledger**. **Choose each slice's maturity rung** from `${config.strategy.maturity_levels}` under
  `${config.strategy.maturity_scope}` (`per_slice` default): a new/unproven slice runs `mvp` (build
  its walking skeleton); a slice that passed its gate last loop advances to the next rung — so
  different slices may run at different rungs in the same loop. (Legacy `whole_backlog` scope keeps
  one rung for all increments.) Then **write the captured record into `OUTPUT.md`** (what was read,
  the config changes, the backlog, each slice's chosen rung + loop number, and the `INPUT.md`
  resolution table). Never write to `INPUT.md`.
- **`OUTPUT.md` (process writes).** Intake seeds it; after each increment Report overwrites it
  with a short, structured status checklist (loop; **per-slice** rung + checkbox + stage + gate +
  the five test levels + debt/deferred + next action), a **provisional-vs-stable contract** list, an
  **assumption-debt** log, a **Resolution of `INPUT.md`** table (resolved / partial / queued), and
  each slice's **next rung**. The loop re-reads `INPUT.md` (read only) for new ideas every cycle.
  **Re-run the workflow to climb the ladder** until `INPUT.md` is fully resolved.

## Recommended model per phase

When your harness supports per-phase models (`${config.models}`), use a stronger tier for
judgment-heavy phases and a cheaper one for mechanical work. Shipped default:

| Phase | Recommended tier |
|-------|------------------|
| 0 · Intake / Report (MVP loop) | sonnet |
| 1 · Requirements | sonnet |
| 2 · Software System | **opus** |
| 3 · Architecture (provisional) | **opus** |
| 4 · Design (provisional) | **opus** |
| Slice Selection (walking skeleton) | sonnet |
| Scaffold (partial: publish THIS slice's interfaces + build skeleton) | sonnet |
| 5 · Implementation (TDD) | sonnet |
| Per-tier integration (module / software / system) | sonnet |
| 6–10 · Unit / Component / Module / System / Acceptance tests | haiku |
| Adaptation (promote provisional→stable, revise) | **opus** |
| Iteration Gate | **opus** |

Refactoring is **on-demand inside every phase** (no separate tier).

On a single-model harness (e.g. Copilot CLI / manual), treat this as guidance — switch model
manually for the opus phases if your tool allows, otherwise run the whole loop on one capable model.

---

## Roles (one responsibility each)

| Role | Owns | Hands off |
|------|------|-----------|
| Requirements Analyst | Stage 1 + acceptance test spec | to Systems Architect |
| Systems Architect    | Stage 2: topology + deployable executables | to Architect |
| Architect            | Stage 3: per-deployable pattern + modules (+ packaging: static/shared·DLL/header-only) + module/system test plans — contracts marked **provisional** | to Designer |
| Designer             | Stage 4: per-component interface + units + component test spec, contracts **provisional** (returns DATA only) | to Slice Planner |
| Slice Planner        | Pick the walking-skeleton **vertical slice** (obvious + architecturally anchored); seed the assumption-debt ledger | to Scaffolder |
| Scaffolder           | Single writer: publish **this slice's** interfaces + a glob build skeleton onto the working branch (partial, grows each cycle) | to Implementer |
| Implementer (TDD)    | Stage 5 code + unit tests — each **unit** on its OWN branch (developer-style) | to Integrator |
| Integrator           | Per tier: merge each VERIFIED node into its parent branch (unit→component→module→software→system→main) | to Verifier |
| Verifier             | Verifies each node IN ISOLATION on its branch (unit→component→module→system→acceptance) | to Reconciler |
| Reconciler           | **Adaptation**: promote validated contracts provisional→stable, revise disproven ones, retire stubs, re-sync traceability + assumption debt | to Gatekeeper |
| Gatekeeper           | Quality gates + Definition of Done | next slice / increment |

In the agentic version each role is a subagent; verification roles are run by
*different* agents than the ones that produced the work (adversarial check).
Refactoring is **on-demand within each role's phase**, not a separate role.

---

## Forward construction, backward learning, clear gates, targeted repair (applies across all stages)

The left arm flows **forward** (Requirements → Software System → Architecture → Design →
Implementation) to set *intent*, but its contracts are **provisional**, not frozen, and there are
**no *uncontrolled* backward jumps**. Real gaps surface in the **test phases** (clear gates: the
climb proceeds only once a level is green); what the running code teaches is fed back **up** *only*
through the **Adaptation** step (below) — promote validated contracts to **stable**, revise the
disproven ones (re-verifying just the affected nodes, bounded by `${config.hybrid.max_adaptation_rounds}`),
and keep traceability + assumption-debt in sync. So: **forward construction, backward learning
through a controlled gate** — never a silent rewrite of an upstream stage.

When a gate goes red, apply a **targeted repair** — repeat the build loop for **only the failing
element** and re-verify, bounded by `${config.agile.max_fix_rounds}`. This is **abstract and identical
at every level**: a failing unit → repeat that unit's red→green→refactor loop; a failing component →
that component; a failing module → that module's wiring; a failing deployable → that deployable.
If a repair reveals a wrong **provisional contract** (not a coding bug), that is fixed at the
Adaptation step, not worked around. **Already-passing siblings are never re-implemented.** If a level
stays red after the fix budget, the increment fails its gate and re-loops (per
`${config.agile.max_gate_retries}`).

---

## Stage 1 — Software Requirements  →  (10) Acceptance Test

**Entry:** a backlog item `{id, title, acceptance}`.
**Do:**
- Restate the need: actor, goal, value. Capture functional + non-functional reqs.
- Assign a stable requirement ID to each (`REQ-<INC>-n`) for traceability.
- Write the **acceptance test specification** (Given/When/Then) that will prove
  the increment end-to-end, using `${config.toolchain.test_frameworks.acceptance}`.
**Exit criteria:** every requirement is testable and has a draft acceptance scenario.
**Prompt template:**
> You are the Requirements Analyst. For backlog item `${item}`, produce: (a) a numbered
> list of functional and non-functional requirements with IDs `REQ-${item.id}-n`;
> (b) acceptance criteria in Given/When/Then form; (c) an acceptance-test spec for
> `${config.toolchain.test_frameworks.acceptance}`. Do not design a solution.

---

## Stage 2 — Software System  →  (9) System Test

**Entry:** approved requirements.
**Do — define the TOP of the composition hierarchy (the system as a whole):**
- Choose the system **topology** (e.g. standalone, **client-server**, service + CLI) and justify it.
- Decompose the system into its **deployables** — the concrete **executables** it ships as. A
  client-server topology yields **two executables** (a client and a server). Give each
  `{name, kind (executable/service/library), responsibility, interface}`. A software/executable **has an
  interface too** — it just looks different: how it's driven and talks (CLI args, network protocol/port,
  IPC, public API).
- Capture the system **context**, **external interfaces**, and key **quality scenarios**.
- Write the **system test plan**: how the deployables run **together** in the topology.
**Exit criteria:** topology chosen; every deployable (executable) named with a responsibility; the
system test plan covers how the executables interact.
**Prompt template:**
> You are the Systems Architect. From the requirements, choose the system topology and decompose it
> into deployable executables (`{name, kind, responsibility}`). Capture context, external interfaces,
> quality scenarios, and a system test plan (how the deployables run together).

---

## Stage 3 — Software Architecture  →  (8) Module Test (mocked)

**Entry:** an approved software system (topology + deployables).
**Do — architect EACH DEPLOYABLE on its own:**
- For **every deployable**, **pick a pattern from the catalog**
  `${config.references.software_architecture_patterns.catalog}` (Layered, Microservices,
  Event-Driven, Repository, CQRS, DDD, …) and justify it — e.g. the client may be **Layered with
  three modules**; the server its own pattern.
- Decompose each deployable into **modules** (record each module's `deployable`, and its `packaging` —
  static library / shared library·DLL / header-only, default static) with clear boundaries; apply
  `${config.clean_code.architecture}` and the dependency rule (`${config.clean_code.dependency_rule}`). One
  or more modules compose a deployable; one or more deployables compose the system.
- Define inter-module interface contracts; record decisions as short ADRs (context → decision →
  consequences), **each tagged `provisional`** until running code validates it. **Document using the
  arc42 template** `${config.references.documentation.sections}`.
- Emit the coarse **component_plan** (the work-list of components per module — `{name, modules,
  responsibility}`) that seeds Stage 4.
- Write **two** test plans: the **module test plan** (how each module's components compose into the
  module — *mocked* at the module boundary, using `${config.toolchain.test_frameworks.integration}`)
  and the **system test plan** input (how modules compose into their deployable).
**Exit criteria:** each deployable has a named pattern; each module maps to a deployable; each
requirement maps to ≥1 module; the component_plan covers every module; arc42 doc drafted; **all
contracts/ADRs marked `provisional`** (they harden only at the Adaptation step).
**Prompt template:**
> You are the Architect. For each deployable in the software system, choose an architecture pattern
> from `${config.references.software_architecture_patterns.catalog}` and justify it. Decompose each
> deployable into modules (with `deployable`), boundaries, and interface contracts honoring
> `${config.clean_code.architecture}` and the inward dependency rule. Treat every contract/ADR as a
> PROVISIONAL hypothesis (mark it `provisional`). Emit ADRs, the module test plan, the component_plan,
> and map each REQ to a module.

---

## Stage 4 — Software Design  →  (7) Component Test (mocked)

**Entry:** approved architecture (modules + the component work-list).
**Do — make the lower composition hierarchy explicit (`${config.v_model.composition}`):**
- Design each **component**: classes/functions, interface, data structures, applied
  design patterns **chosen from the catalog** `${config.references.design_patterns}`
  (Creational/Structural/Behavioral — Adapter, Strategy, Factory Method, Observer, …);
  justify each by the problem it solves — no pattern for its own sake (avoid the
  _Speculative Generality_ smell).
- **unit(s) → component:** list the **units** that make up each component, each with its own **interface**
  (the unit's signature/contract — what its implementer codes against and what sibling units mock) and a
  unit-test spec. A unit belongs to **exactly one** component.
- **component(s) → module:** record the module(s) each component is assigned to. A component
  **may serve several modules** — reuse it (it is implemented once), don't duplicate it.
- Specify error handling per `${config.clean_code.error_handling}` and resource
  ownership per `${config.clean_code.resource_management}`.
- Write **component test specs** (contract-level, collaborators mocked with
  `${config.toolchain.test_frameworks.unit.mock}`).
**Exit criteria:** every component lists its units and the module(s) it serves; every public
interface has a contract (tagged `provisional`) and a component test spec.
**Prompt template:**
> You are the Designer. Specify components with their public interfaces (tagged `provisional`), data
> structures, and justified design patterns. For each component, list its **units** (each with a
> unit-test spec; a unit belongs to exactly one component) and the **module(s)** it is assigned to
> (one or more — shared components are reused, not duplicated). Define error handling and ownership.
> Produce component-test specs. Treat all contracts as provisional hypotheses.

---

## Stage 4b — Slice Selection (the walking skeleton)

**Entry:** the provisional design (components + units across the whole increment).
**Do — choose the bottom-up seed (`${config.hybrid.slice_selection}`, `${config.hybrid.walking_skeleton}`):**
- Pick the **thinnest end-to-end vertical slice** that crosses *every* tier
  (unit → component → module → software → system → acceptance) and exercises the **riskiest, most
  load-bearing** provisional assumptions. It must be **obvious AND architecturally anchored** — every
  unit in the slice attaches to a requirement, this slice, a risk probe, or a known architectural
  seam. Never seed from arbitrary easy utilities (reverse-YAGNI).
- List the exact units/components in the slice; the rest of the system stays a **provisional seam**
  (built in later cycles).
- **Seed the assumption-debt ledger** (`${config.living_artifacts.assumption_debt}`): every stub /
  driver / mock the slice will need, each with an **owner** and a **retirement condition**.
**Exit criteria:** one vertical slice selected, mapped to ≥1 requirement, crossing all tiers; its
units listed; the assumption-debt ledger seeded.
**Prompt template:**
> You are the Slice Planner. From the provisional design, select the WALKING SKELETON: the thinnest
> end-to-end vertical slice of REAL units that proves the riskiest architectural assumptions. It must
> map to a requirement and be architecturally anchored (no arbitrary utilities). List the slice's
> units/components, mark everything else a provisional seam, and seed the assumption-debt ledger
> (each stub/provisional interface with an owner + retirement condition).

---

## Stage 5 — Software Implementation  →  (6) Unit Test (TDD)

**Entry:** the selected slice (its components + units) and the partial Scaffold.
**Branch-per-unit & parallel:** Design returns contracts as data, **Slice Selection** picks the
walking skeleton, then the **partial Scaffold** publishes *that slice's* interfaces + build skeleton;
only then does implementation fan out — **each unit on its own branch** (developer-style), coding only
against the **published** interfaces (mock collaborators, including sibling units; stubs are logged as
assumption debt). The Component Tier later merges a component's unit branches into the component
branch. State is **partial across loops** — reuse and EXTEND existing units, don't rebuild.
**Do, per unit (red→green→refactor):**
1. Write the failing unit test from the component contract
   (`${config.toolchain.test_frameworks.unit.tool}`).
2. Write the minimum `${config.language.name}` `${config.language.standard}` code to pass.
3. Tidy locally (naming, small functions) keeping the test green; refactor on demand.
- Place code under `${config.layout.source_dir}` / `${config.layout.include_dir}`,
  tests under `${config.layout.test_dir}`. Touch only THIS component's files.
- Run `${config.toolchain.formatter.fix_command}` and the linters as you go.
**Exit criteria:** all of this component's unit tests green; no linter/formatter findings on touched files.
**Prompt template:**
> You are the Implementer using TDD. Implement component `${c}` in
> `${config.language.name} ${config.language.standard}`. For each unit: write the failing test first,
> then minimal code. Reuse/extend existing code (don't re-implement passing units); mock collaborators.
> Obey `${config.clean_code.*}`. Run `${config.toolchain.formatter.tool}` and `${config.toolchain.linters[*].tool}`.

---

## Scaffold (single writer, PARTIAL) — publish THIS slice's interfaces + build skeleton

Design returns each component's contract as **data only** (it writes nothing, so the parallel designers
never race). Then exactly ONE writer prepares the working branch *before* implementation fans out —
publishing only the **current slice's** seams (`${config.hybrid.partial_scaffold}`), and **growing the
skeleton cycle by cycle** as later slices are built and contracts harden:

- Publish the slice's component **interfaces** (headers/contracts under `${config.layout.include_dir}`,
  component-test specs under `${config.layout.test_dir}`) so any implementer can **mock any collaborator**.
  Interfaces outside the slice stay **provisional seams** until their slice is built.
- Establish a **hierarchical build that mirrors the composition tree** — one `CMakeLists.txt` **per
  component, per module, and per executable**, composed bottom-up via `add_subdirectory` (a component's
  CMake globs its unit sources + tests; its module pulls in its components; the executable its modules; the
  root the system). **Units have no build file of their own** — they compile inside their component's CMake
  (a single unit uses a *temporary* throwaway target during TDD). So **every node builds and tests in
  isolation**: a unit/component/module never builds the whole project (only the acceptance test does), and
  adding a file is a *local* edit to that node's CMake. A module may be a **static library, a shared
  library / DLL, or header-only** — the architecture's per-module choice (default static).
- Reuse/extend what already exists across loops; never clobber working code. Commit once.

This single-writer step is what keeps every later merge a **disjoint, conflict-free add**: the interfaces
and the skeleton are the only shared artifacts, and they are on the branch *before* anyone forks from it.
Because it is **partial**, the agent never has to invent the *entire* system's interfaces before a line of
code validates any of them — only the slice's. Implementation (Stage 5) then fans out with **one unit per
branch**; the bottom-up gated merges happen during verification (Stages 6–10 below), starting with the
Component Tier merging a component's unit branches. Each phase still writes its trace file (below).

---

## Per-phase persistence — commit + trace (every stage)

This applies to **every** stage above, not a stage of its own:

- **Commit-per-phase (`${config.git.commit_per_phase}`).** When a phase finishes, persist its
  content and commit it onto the current branch — message
  `${config.git.commit_prefix}(<level>/<INC>): <Phase>`. Progress lands on `main` incrementally and
  the loop is resumable. (Implementation commits happen inside each worktree; Integrate brings them onto `main`.)
- **Trace file (always).** Each phase also writes a small markdown file to
  `${config.living_artifacts.phase_trace}` — key outputs/decisions, anything deferred, files touched,
  one-line status. This happens **even when `${config.toggles.documentation}` is `minimal` or `off`**:
  it is process telemetry (a file-level trail), not product documentation, so the doc toggle never
  switches it off. It also guarantees each per-phase commit is non-empty.

---

## Stages 6–10 — Verification & integration (a bottom-up tree of gated merges)

The right arm is a **tree of gated merges that mirrors the composition tree**. Each node is built/
integrated **on its own branch**, verified **in isolation** by its tier's test, and merged into its
**parent branch only once green** — unit → its component branch, component → its module branch, module → its
software (executable) branch, software → the system branch, and the verified system → the working branch
(`main`). So `main` only ever receives fully-verified work and siblings never collide — which is what lets
**very complex systems** integrate cleanly.

Each tier first **pulls the previous tier's verified child branches up** (an Integrator creates the parent
branch, `git merge --no-ff`s the verified children in, writes that tier's glue — linking static libs,
resolving shared libs/DLLs — and confirms it builds), then a **different agent than the implementer**
verifies each node in isolation (adversarial). **Each verifier builds and runs ONLY its own node's
target** (its CMake) — never the whole project; only the acceptance test builds the full system. Run the
levels in the order `${config.v_model.test_execution_order}`; each level is a **barrier/gate** before the next:

6. **Unit tests** — the Component Tier's Integrator first merges the component's **unit branches** into the
   component branch; then *one verifier per component* builds it **once** and runs all its unit tests as a
   **batch**, with `${config.toolchain.sanitizers}` enabled; collect coverage via `${config.toolchain.coverage.tool}`.
7. **Component tests** — *one verifier per component*, each against its contract, collaborators **mocked**.
8. **Module tests** — *one verifier per module*: its components compose; OTHER modules **mocked** at the boundary.
9. **System tests** — *one verifier per deployable*: its modules compose into the executable, and it
   participates correctly in the topology with the other deployables (the system test plan).
10. **Acceptance test** — *one verifier for the whole running system* (the ONE place the full system is
    built and run), NO mocking: the Stage-1 scenarios end to end. The validator is **expected to devise a way
    to capture evidence** — for a GUI/OpenGL app, capture the rendered window (screen/window capture or a
    framebuffer/PNG dump); for a CLI, recorded output / exit codes. On green, the verified **system branch
    merges onto `main`** and the increment's per-node worktrees/branches are pruned.

**A different agent than the implementer runs these** (adversarial verification).

**Clear gates + targeted repair.** Each level is a gate; the climb proceeds only when it is green.
When a verifier goes red, repeat the build loop for **only the failing element** at that level and
re-verify — bounded by `${config.agile.max_fix_rounds}` rounds. This is **abstract and identical at
every level**: a failing unit → repeat that unit's loop; a failing component → that component; a
failing module → that module's wiring; a failing deployable → that deployable. **Already-passing
siblings are never re-implemented** — green stays green.

| Red gate | Targeted repair — repeat the loop for… |
|----------|----------------------------------------|
| unit (6) | the failing **unit(s)** |
| component (7) | the failing **component(s)** |
| module (8) | the failing **module(s)** |
| system (9) | the failing **deployable(s)** |
| acceptance (10) | the specific behavior across the running system |

If a level is still red after `${config.agile.max_fix_rounds}` fix rounds, the climb stops and the
increment fails its gate (then re-loops per `${config.agile.max_gate_retries}`). Because siblings at a
level are independent, the verifiers — and the targeted fixes — **parallelize**.

**Refactoring is on demand** within any phase (no separate refactor stage): when an agent spots a smell
from `${config.references.refactoring.smells}`, it applies the matching technique from
`${config.references.refactoring.techniques}` and keeps tests green. The clean-code thresholds
(complexity, function length, dependency rule) are enforced at the Iteration Gate below.

---

## Adaptation (controlled feedback up) — runs after the slice is green, before the Gate

**Entry:** the slice's test levels are all green (bottom-up evidence collected).
**Do (`${config.v_model.adaptation}`, model `${config.models.adaptation}`):** let what the running
code taught flow **up** — the one legitimate, bounded backward path:
- **Promote** every `provisional` contract/ADR the slice's tests validated to **`stable`**.
- **Revise** the provisional contracts the code *disproved* (the design hypothesis was wrong), then
  **re-verify only the affected nodes** — bounded by `${config.hybrid.max_adaptation_rounds}` rounds.
- **Retire** stubs/drivers/mocks whose **retirement condition** in the assumption-debt ledger is now
  met (the real collaborator exists), and re-point dependents at the real interface.
- **Re-sync** the traceability matrix and the assumption-debt ledger to the new reality.
**Exit criteria:** no `provisional` contract remains *inside this slice* if it reached `complete`;
affected nodes re-verified green; traceability matrix + assumption-debt ledger updated and committed.
**Prompt template:**
> You are the Reconciler. The slice's tests are green. Using the running code as evidence: promote
> validated provisional contracts/ADRs to `stable`; revise any the code disproved and re-verify only
> the affected nodes (≤ `${config.hybrid.max_adaptation_rounds}` rounds); retire stubs whose
> retirement condition is met; re-sync the traceability matrix and assumption-debt ledger. Do NOT
> silently rewrite upstream stages — record every promotion/revision as a short ADR update.

---

## Iteration Gate (Definition of Done)

Judge against the **effective gates for this slice's maturity rung** (the strict
`${config.quality_gates}` with the rung's relaxations merged on top). Intentionally-deferred
behavior is **not** a failure at a lower rung — record it as debt for a later loop, don't block on it.
Pass the increment only if **all** hold (else re-loop, bounded by `${config.agile.max_gate_retries}`):

- [ ] All five test levels green (unit, component, module, system, acceptance).
- [ ] Unit line coverage ≥ `${config.quality_gates.unit_line_coverage_min}`%, branch ≥ `${config.quality_gates.unit_branch_coverage_min}`%.
- [ ] `${config.toolchain.linters[*].tool}` findings ≤ their `*_warnings_max`.
- [ ] Formatter check `${config.quality_gates.format_check}`.
- [ ] Sanitizers clean (`${config.quality_gates.sanitizers_clean}`).
- [ ] Public API doc coverage ≥ `${config.quality_gates.public_api_doc_coverage_min}`% (`${config.toolchain.docs.tool}`).
- [ ] Complexity & function-length within limits.
- [ ] Traceability matrix **produced** (persisted to `${config.living_artifacts.traceability}`): every REQ → ≥1 acceptance test.
- [ ] **Adaptation done:** no `provisional` contract remains inside a slice that reached `complete`.
- [ ] **Assumption-debt ledger** up to date: every open stub/provisional interface has an owner + retirement condition (and any whose condition was met are retired).
- [ ] Definition of Done items in `${config.agile.definition_of_done}` checked.

**Output of the increment:** working software + the produced traceability matrix + the updated
assumption-debt ledger + a short increment report (what shipped, gate results, refactors applied,
provisional→stable promotions, debt logged).

---

## Deliverable templates

**Traceability matrix (PRODUCED artifact, one row per requirement — persisted to `${config.living_artifacts.traceability}`):**

| REQ ID | Requirement | Module | Component | Unit tests | Component tests | Module tests | System tests | Acceptance test | Status |
|--------|-------------|--------|-----------|------------|-----------------|--------------|--------------|-----------------|--------|

**Assumption-debt ledger (one row per open stub / provisional interface — persisted to `${config.living_artifacts.assumption_debt}`):**

| ID | Kind (stub/driver/mock/provisional-iface) | Slice | Owner | Retirement condition | Status (open/retired) |
|----|-------------------------------------------|-------|-------|----------------------|-----------------------|

**ADR (one per significant decision — carries a state):**

```
# ADR-<n>: <title>   [state: provisional | stable]
Context:  <forces, constraints>
Decision: <what we chose>
Consequences: <trade-offs, what becomes easier/harder>
Evidence: <which slice's tests validated/revised this; n/a while provisional>
```

**Increment report:**

```
Increment: <id> <title>   (slice: <slice>, rung: <mvp|harden|complete>)
Shipped:   <summary>
Gates:     coverage <x>% | lint <n> | sanitizers <ok|fail> | docs <x>%
Refactors: <list>
Promotions: <provisional→stable contracts this loop>
Debt:      <logged items + follow-up increment>  | Assumptions: <open stubs + retirement conditions>
```
