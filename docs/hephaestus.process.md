# Hephaestus — Portable Process Specification

> Tool-agnostic, step-by-step procedure for running **one increment** of the
> Cyclic Agentic Agile V-Model. Follow this by hand, with any agent harness, or
> as the contract the automated workflow implements. Every `${config.x}` refers
> to [`../config/hephaestus.config.yaml`](../config/hephaestus.config.yaml).
>
> Repeat this whole procedure once per entry in `${config.project.backlog}`.
>
> **MVP first, deepen by re-running (`${config.strategy}`).** Hephaestus is meant to be
> **run several times**. One run advances the product by exactly **one maturity rung**
> (`${config.strategy.maturity_levels}` — `mvp → harden → complete`) across the whole
> backlog: the first run builds the thinnest end-to-end MVP (happy path, edge cases
> deferred as debt), and each later run reads the state in `OUTPUT.md` and **deepens**
> until every idea in `INPUT.md` is fully resolved at the top rung. Each rung may relax
> the quality gates; the **effective gates = `${config.quality_gates}` with that rung's
> overrides merged on top**.
>
> **Commit per phase, on the working branch (`${config.git}`).** Every phase persists its
> content and commits it onto the current branch as it finishes, so progress lands on
> `main` incrementally and the loop is resumable. Parallel implementation runs in isolated
> git worktrees; their branches are merged back in an **Integrate** step before verification.
> And **every phase writes a small trace file** (`${config.living_artifacts.phase_trace}`)
> **regardless of the documentation toggle** — a file-level trail, and it keeps each commit
> non-empty.
>
> **Carry-forward:** every stage writes a *persisted* artifact (`${config.living_artifacts}`).
> Start each increment by loading the prior ADRs, the traceability matrix, and the debt
> log — they are the shared memory of the loop for **both human and agent**, and last
> cycle's decisions/debt are this cycle's constraints (`${config.carry_forward}`).
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
  loops' state) and **rewrite the project files to match**: map high-level choices into
  `config/hephaestus.config.yaml` (language, toolchain, quality_gates, toggles.documentation,
  models, …) and turn feature ideas into backlog increments. **Choose this loop's maturity rung**
  from `${config.strategy.maturity_levels}`: the first loop runs `mvp`; each later loop picks the
  lowest rung not yet completed for the whole backlog. Then **write the captured record into
  `OUTPUT.md`** (what was read, the config changes made, the backlog, the chosen rung + loop number,
  and the `INPUT.md` resolution table). Never write to `INPUT.md`.
- **`OUTPUT.md` (process writes).** Intake seeds it; after each increment Report overwrites it
  with a short, structured status checklist (loop + maturity rung; per-increment checkbox + rung
  reached + stage + gate + the five test levels + debt/deferred + next action), a **Resolution of
  `INPUT.md`** table (resolved / partial / queued), and the **next loop's rung**. The loop re-reads
  `INPUT.md` (read only) for new ideas every cycle. **Re-run the workflow to climb the ladder** until
  `INPUT.md` is fully resolved.

## Recommended model per phase

When your harness supports per-phase models (`${config.models}`), use a stronger tier for
judgment-heavy phases and a cheaper one for mechanical work. Shipped default:

| Phase | Recommended tier |
|-------|------------------|
| 0 · Intake / Report (MVP loop) | sonnet |
| 1 · Requirements | sonnet |
| 2 · Software System | **opus** |
| 3 · Architecture | **opus** |
| 4 · Design | **opus** |
| 5 · Implementation (TDD) | sonnet |
| Integrate (on demand) | sonnet |
| 6–10 · Unit / Component / Module / System / Acceptance tests | haiku |
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
| Architect            | Stage 3: per-deployable pattern + modules + module/system test plans | to Designer |
| Designer             | Stage 4: per-component interface + units + component test spec | to Implementer |
| Implementer (TDD)    | Stage 5 code + unit tests (in isolated worktrees) | to Integrator |
| Integrator           | On demand: assemble units→…→system; merge worktrees onto the working branch | to Verifier |
| Verifier             | Runs unit→component→module→system→acceptance | to Gatekeeper |
| Gatekeeper           | Quality gates + Definition of Done | next increment |

In the agentic version each role is a subagent; verification roles are run by
*different* agents than the ones that produced the work (adversarial check).
Refactoring is **on-demand within each role's phase**, not a separate role.

---

## Forward decomposition, clear gates, targeted repair (applies across all stages)

The left arm flows **forward only** (Requirements → Software System → Architecture → Design →
Implementation) — **no backward jumps** between design stages. Any real gap surfaces in the **test
phases**, which are **clear gates**: the climb proceeds only once a level is green.

When a gate goes red, apply a **targeted repair** — repeat the build loop for **only the failing
element** and re-verify, bounded by `${config.agile.max_fix_rounds}`. This is **abstract and identical
at every level**: a failing unit → repeat that unit's red→green→refactor loop; a failing component →
that component; a failing module → that module's wiring; a failing deployable → that deployable.
**Already-passing siblings are never re-implemented.** If a level stays red after the fix budget, the
increment fails its gate and re-loops (per `${config.agile.max_gate_retries}`).

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
  `{name, kind (executable/service/library), responsibility}`.
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
- Decompose each deployable into **modules** (record each module's `deployable`) with clear
  boundaries; apply `${config.clean_code.architecture}` and the dependency rule
  (`${config.clean_code.dependency_rule}`). One or more modules compose a deployable; one or more
  deployables compose the system.
- Define inter-module interface contracts; record decisions as short ADRs (context → decision →
  consequences). **Document using the arc42 template** `${config.references.documentation.sections}`.
- Emit the coarse **component_plan** (the work-list of components per module — `{name, modules,
  responsibility}`) that seeds Stage 4.
- Write **two** test plans: the **module test plan** (how each module's components compose into the
  module — *mocked* at the module boundary, using `${config.toolchain.test_frameworks.integration}`)
  and the **system test plan** input (how modules compose into their deployable).
**Exit criteria:** each deployable has a named pattern; each module maps to a deployable; each
requirement maps to ≥1 module; the component_plan covers every module; arc42 doc drafted.
**Prompt template:**
> You are the Architect. For each deployable in the software system, choose an architecture pattern
> from `${config.references.software_architecture_patterns.catalog}` and justify it. Decompose each
> deployable into modules (with `deployable`), boundaries, and interface contracts honoring
> `${config.clean_code.architecture}` and the inward dependency rule. Emit ADRs, the module test plan,
> the component_plan, and map each REQ to a module.

---

## Stage 4 — Software Design  →  (7) Component Test (mocked)

**Entry:** approved architecture (modules + the component work-list).
**Do — make the lower composition hierarchy explicit (`${config.v_model.composition}`):**
- Design each **component**: classes/functions, interface, data structures, applied
  design patterns **chosen from the catalog** `${config.references.design_patterns}`
  (Creational/Structural/Behavioral — Adapter, Strategy, Factory Method, Observer, …);
  justify each by the problem it solves — no pattern for its own sake (avoid the
  _Speculative Generality_ smell).
- **unit(s) → component:** list the **units** that make up each component, each with a
  unit-test spec. A unit belongs to **exactly one** component.
- **component(s) → module:** record the module(s) each component is assigned to. A component
  **may serve several modules** — reuse it (it is implemented once), don't duplicate it.
- Specify error handling per `${config.clean_code.error_handling}` and resource
  ownership per `${config.clean_code.resource_management}`.
- Write **component test specs** (contract-level, collaborators mocked with
  `${config.toolchain.test_frameworks.unit.mock}`).
**Exit criteria:** every component lists its units and the module(s) it serves; every public
interface has a contract and a component test spec.
**Prompt template:**
> You are the Designer. Specify components with their public interfaces, data structures, and
> justified design patterns. For each component, list its **units** (each with a unit-test spec;
> a unit belongs to exactly one component) and the **module(s)** it is assigned to (one or more —
> shared components are reused, not duplicated). Define error handling and ownership. Produce
> component-test specs.

---

## Stage 5 — Software Implementation  →  (6) Unit Test (TDD)

**Entry:** approved design (per-component interface + units).
**Interface-first & parallel:** Stages 4 and 5 form a **per-component pipeline** — once a component's
interface is designed it can be implemented while OTHER components are still being designed. Each
component is implemented in its own **isolated git worktree**, coding only against interfaces (mock
collaborators). State is **partial across loops** — reuse and EXTEND existing units, don't rebuild.
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

## Integrate (on demand) — assemble bottom-up onto the working branch

Integration happens **when there is something to merge** (the parallel per-component worktrees). It
assembles the composition hierarchy **bottom-up** — units → components → modules → deployables
(executables) → system:

- `git worktree list --porcelain` to discover the branches; `git merge --no-ff` each one.
- Resolve conflicts so **all** components survive; reconcile shared files (build config, shared
  headers, test registration). Never drop a component; a shared component is integrated once.
- **Assemble the hierarchy in the build**: group each module's components into its module target, each
  module into its **deployable (executable)** target, and wire the deployables into the **system**
  (topology) — so unit → component → module → deployable → system is reflected in the build structure.
- Run the formatter and a build to confirm every executable compiles and links.
- Prune merged worktrees (`git worktree remove`), then commit the integration.

If components were implemented inline (no worktrees), just ensure everything is committed on the
working branch. Either way, write the phase trace file (below).

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

## Stages 6–10 — Verification (climb the V, bottom-up — fan out, then feed back)

Run the levels in the order `${config.v_model.test_execution_order}`. Each level **mirrors the
composition hierarchy and fans out in parallel**, then acts as a **barrier** before the next:

6. **Unit tests** — *one verifier per component* (its units), fast, isolated, with
   `${config.toolchain.sanitizers}` enabled; collect coverage via `${config.toolchain.coverage.tool}`.
7. **Component tests** — *one verifier per component*, each against its contract, collaborators **mocked**.
8. **Module tests** — *one verifier per module*: its components compose; OTHER modules **mocked** at the boundary.
9. **System tests** — *one verifier per deployable*: its modules compose into the executable, and it
   participates correctly in the topology with the other deployables (the system test plan).
10. **Acceptance test** — *one verifier for the whole running system*, NO mocking: the Stage-1 scenarios
    end to end, with concrete **evidence** (e.g. screenshots / recorded output / exit codes).

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

## Iteration Gate (Definition of Done)

Judge against the **effective gates for this loop's maturity rung** (the strict
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
- [ ] Traceability matrix: every REQ → ≥1 acceptance test.
- [ ] Definition of Done items in `${config.agile.definition_of_done}` checked.

**Output of the increment:** working software + the traceability matrix + a short
increment report (what shipped, gate results, refactors applied, debt logged).

---

## Deliverable templates

**Traceability matrix (one row per requirement):**

| REQ ID | Requirement | Module | Component | Unit tests | Component tests | Integration tests | Acceptance test | Status |
|--------|-------------|--------|-----------|-----------|-----------------|--------------------|-----------------|--------|

**ADR (one per significant decision):**

```
# ADR-<n>: <title>
Context:  <forces, constraints>
Decision: <what we chose>
Consequences: <trade-offs, what becomes easier/harder>
```

**Increment report:**

```
Increment: <id> <title>
Shipped:   <summary>
Gates:     coverage <x>% | lint <n> | sanitizers <ok|fail> | docs <x>%
Refactors: <list>
Debt:      <logged items + follow-up increment>
```
