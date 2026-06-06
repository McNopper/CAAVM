# Hephaestus — Plan & Methodology

> The **Cyclic Agentic Agile V-Model**. ([Why "Hephaestus"?](https://en.wikipedia.org/wiki/Hephaestus) — the Greek god of the forge who built automatons.)

> A reusable, language-agnostic methodology for building software increment by
> increment, where **every design stage is paired with the test level that
> verifies it** (the V), the V is **repeated each iteration** (the cycle), and
> **autonomous agents** drive each stage under enforced **clean-code and
> refactoring** gates.
>
> All language/tool specifics live in [`../config/hephaestus.config.yaml`](../config/hephaestus.config.yaml).
> This document never hard-codes C++ — it reads `${config.*}` placeholders.

---

## 1. Why "Cyclic Agentic Agile V-Model"

| Word | What it contributes |
|------|---------------------|
| **Cyclic**  | The full V is re-run for *each* backlog increment, not once for the whole project. |
| **Agentic** | Each stage is executed by a dedicated agent with a single responsibility; verification is adversarial (separate agents try to break the work). |
| **Agile**   | Small vertical slices, working software every iteration, refactoring as a first-class step. |
| **V-Model** | Strict traceability: requirements ↔ acceptance, software system ↔ system, architecture ↔ module, design ↔ component, implementation ↔ unit. |

The classic V-Model's weakness is that it is waterfall — one giant pass. Hephaestus
keeps the V-Model's traceability discipline but **folds it into the agile loop**:
one short, complete V per increment.

---

## 2. The shape of one increment (one V-pass)

The V has **five left stages** (decompose, top-down) each paired with the **test level
that verifies it** (right, bottom-up). Phase **0** is the MVP loop that wraps the whole V
(§2b); refactoring and integration are **on-demand**, not fixed phases (§2c).

```
 LEFT ARM (decompose & design)                              RIGHT ARM (verify, bottom-up)
 ─────────────────────────────                              ─────────────────────────────
 (1) Requirements ─────────────────────────────────────────► (10) Acceptance test
        │  what & why, acceptance criteria                          run the whole system, end-to-end
        ▼                                                           (e.g. screenshots / E2E)
 (2) Software System ──────────────────────────────────────► ( 9) System test
        │  topology + deployables (executables),                   deployables run together in the topology
        │  e.g. client-server → client + server                    ▲
        ▼                                                          │
 (3) Architecture ─────────────────────────────────────────► ( 8) Module test (mocked)
        │  PER deployable: pattern + modules                       modules compose into the deployable
        ▼                                                          ▲
 (4) Design ───────────────────────────────────────────────► ( 7) Component test (mocked)
        │  PER component: interface + units                        component honors its contract
        ▼                                                          ▲
 (5) Implementation ───────────────────────────────────────► ( 6) Unit test
        code (TDD), PER component                                  each unit is correct
                                                                   ▲
   Scaffold (publish interfaces + skeleton) · each node verified on its branch, merged up only when green · Refactor (any phase) · then ITERATION GATE
   gate pass ▸ next increment ·  gate fail ▸ re-loop (bounded)
```

The composition hierarchy these stages build (each *one or more*):

```
software system ─┬─ software (executable) ─┬─ module ─┬─ component ─┬─ unit
  (topology)     │   e.g. client, server   │          │            │
  e.g. client-   └─ (Software System,       └─ (Arch,  └─ (Design,  └─ (Impl, phase 5)
       server         phase 2)                 phase 3)   phase 4)
```

**Left arm** flows top-down; each stage refines the previous, defines the *interface/contract*
of the next, and *also designs the test* that verifies it. **Right arm** executes bottom-up
in the order `config.v_model.test_execution_order`. The decomposition is **interface-first**,
so once a level's contracts exist (published by the single-writer Scaffold) the work below runs **highly
in parallel** (§3b, §4): each deployable is architected on its own, every **unit** is implemented on its
**own branch** (developer-style), and each test level fans out per sibling. It is assembled back bottom-up
as a **tree of gated merges** — units → components → modules → deployables → system, each node merging into
its parent only once green (§2c).

### The abstract shape — a lattice of gems

Strip away the labels and this is **divide-and-conquer**: *divide* on the way down, *conquer* the leaves
(the units), *combine* on the way up. Each phase pair has one shape — a **down-phase takes one input and
produces one-or-more outputs** (divide, 1→N), and the paired **up-phase takes those several inputs and
produces one output** (combine = integrate + verify, N→1). So every node **splits then rejoins** — a
**gem** ◆ — and because the split recurses all the way down, the whole increment isn't a flat "V" but a
**lattice of nested gems**:

```
              ◆ system            (1 → deployables → 1)
            ◆   ◆   ◆ software    (1 → modules → 1)
          ◆ ◆ ◆ ◆ ◆ ◆ module     (1 → components → 1)
         ◆ ◆ ◆ ◆ ◆ ◆ ◆ component (1 → units → 1)
              · · · units          (the facets)
```

The system gem contains software gems, each containing module gems, then component gems, down to the
units (the facets). Decompose (cut the facets) on the way down; integrate-and-verify (polish them back
together) on the way up. The single-writer Scaffold and the deterministic branch tree are what keep each
gem's facets independent so they can be cut in parallel and rejoin without conflict.

And the **outermost gem is the interface itself**: `INPUT.md` is the single point *in* — Intake fans it out
across the backlog and, inside each increment, down the whole gem lattice into many parallel lines of work
— and everything converges back to the single point *out*, `OUTPUT.md` (re-runs stack further gems as the
product deepens). So the shape is **self-similar at every scale**: the whole run is a gem, each increment is
a gem, each node is a gem — the same 1 → many → 1 from `INPUT.md` all the way down to a unit and back.

**Same operator, different payload.** What is identical everywhere is the *logic*: take one input, decompose
into independent parts, work each in isolation, integrate-and-verify them back into one, gate, emit one
output. What *differs* is only the **activity inside a node** — wording requirements, choosing an
architecture pattern, TDD-ing a unit, wiring a topology. That invariance is the whole trick: the engine is
one small **recursive procedure parameterized per phase**, which is exactly why the workflow's tier walk is
generic (one verify-then-merge loop) with only the agent's prompt swapped per kind. Learn the gem once and
you understand every phase at every scale.

**Contracts are the only coupling.** Because decomposition is strictly **top-down**, a child node is handed
its contract by its parent and needs **nothing else** — not a sibling's internals, not another node's code,
only the **interfaces** it mocks against (published once by Scaffold; Ports & Adapters). A node is
deliberately *blind* to everything outside its own facet. That blindness is not a limitation — it is what
**licenses** cutting all of a gem's facets in parallel and rejoining them without conflict. The interface is
the only thing that ever crosses a node boundary, up or down.

**Every node has an interface — they just look different.** A system's interface is its external
protocols/APIs; a **software (executable)'s** is how it's driven and talks (CLI args, network protocol/port,
IPC, public API); a **module's** is its API contract; a **component's** is its class/function contract; a
**unit's** is its signature. So the entire down-arm is one activity repeated at ever-finer grain — *define
the interface of the next level* — and the **only place real code is written is the leaf unit**, against its
own interface. Decomposition = interface definition; implementation = the leaves. (Each interface is
published by Scaffold so it exists before anyone forks beneath it.)

---

## 2b. The MVP maturity ladder — run several times to resolve `INPUT.md`

Hephaestus does **not** try to build everything in one giant pass. It is meant to be
**re-run**, and **one run advances the product by exactly one maturity rung** across
the whole backlog (`config.strategy`):

```
 INPUT.md ─┐
           ▼
   run #1  ► loop @ mvp       ─ thinnest end-to-end slice; edge cases deferred as debt
   run #2  ► loop @ harden    ─ pull deferred edge cases / non-functional reqs back in
   run #3  ► loop @ complete  ─ strict gates, full robustness; INPUT.md fully resolved
           ▲
           └─ each loop reads OUTPUT.md state, deepens, and re-checks INPUT.md
```

Each rung may **relax the quality gates**; the *effective* gates are
`config.quality_gates` with that rung's overrides merged on top (so `mvp` ships at,
say, 50% coverage and no doc requirement, while `complete` enforces the strict
defaults). The **Intake** step picks the rung for the run by reading the state in
`OUTPUT.md`; the **Report** step records how much of `INPUT.md` is *resolved*
(resolved / partial / queued) and what the next loop will do. So `INPUT.md` is
"resolved in several meaningful loops": jot → run → read `OUTPUT.md` → run again,
until everything is resolved at the top rung. Set `strategy.approach: full` to skip
the ladder and enforce strict gates from loop 1.

This is the MVP discipline folded into the cycle: prove the core works first, then
deepen — never gold-plate ahead of need (YAGNI), never ship throwaway architecture.

## 2c. Gated-merge integration & on-demand refactoring; commit-per-phase

**Integration is a bottom-up TREE OF GATED MERGES.** The git branch tree mirrors the composition tree: a
**unit** branches from its **component** branch (and merges back once its unit test is green); each
**component** branch merges into its **module** branch; each **module** into its **software (executable)**
branch; each **software** into the **system** branch; and the verified **system** finally merges onto the
working branch (`main`). Every node is built/integrated **on its own branch**, verified **in isolation** by
its tier's test, and merged into its parent **only once green** (`git merge --no-ff`) — so siblings never
collide and `main` only ever receives fully-verified work. This per-node isolation is what lets
**arbitrarily complex systems** integrate cleanly. A red node triggers a targeted repair on **that node's
branch only**; already-green siblings are untouched. (A module may be packaged as a static library, a
shared library / DLL, or header-only — the architecture chooses per module.)

**Branch names are deterministic and human-readable**, so no tier has to *discover* anything — it knows
exactly which child branches to merge, and the name itself is a hint (loop, V-stage, kind, node):
`<prefix>/loop<N>/<inc>/<rung>/s<NN>-<kind>/<node>`, e.g. `hephaestus/loop1/inc-001/mvp/s05-component/csv-parser`,
`…/s08-module/core`, `…/s09-software/client`, `…/s10-system`. The whole increment's branches share one
prefix, so they list and prune together; verifiers read a branch via a *detached* throwaway worktree (so a
branch checked out elsewhere is never an obstacle), and per-node scratch worktrees live under a gitignored
`.hephaestus/` dir.

**Single-writer Scaffold before the fan-out.** Right after Design, one step publishes every component
**interface** plus a **glob-based build skeleton** onto the working branch, so each implementer can mock any
collaborator's contract and adding a unit's file needs no edit to shared build config — which keeps every
downstream merge a disjoint, conflict-free add.

**Refactoring is on-demand inside every phase** (not a separate numbered phase): whenever an agent
spots a smell from the catalog while working, it applies the matching technique and keeps tests green
(red → green → refactor). The clean-code thresholds are still enforced at the Iteration Gate.

**Commit-per-phase.** Every phase commits its content onto the working branch as it finishes
(`config.git.commit_per_phase`), so `main` advances incrementally and a run is **resumable**. Every
phase also writes a small **trace file** (`config.living_artifacts.phase_trace`,
e.g. `docs/hephaestus/trace/<INC>/<level>/05-implementation.md`) **regardless of the documentation
toggle** — a file-level trail that also keeps each commit non-empty. The trace is process telemetry;
`minimal`/`off` only scale the *product* documentation, never this trail.

---

## 3. Stages, artifacts, and the test paired with each

| # | Left stage (design) | Primary artifacts | Paired test (right) | Verifies |
|---|---------------------|-------------------|---------------------|----------|
| 1 | **Requirements**     | User stories, acceptance criteria, non-functional reqs, traceability IDs | **(10) Acceptance test** | The system does what the user asked, end to end. |
| 2 | **Software System**  | Topology (e.g. client-server) + the deployable executables, context, external interfaces | **(9) System test** | The executables compose and run together in the topology. |
| 3 | **Architecture**     | Per-deployable pattern + module decomposition, boundaries, dependency rule, ADRs | **(8) Module test** (mocked) | Modules collaborate correctly inside their deployable. |
| 4 | **Design**           | Per-component interface contracts, data structures, applied patterns, units | **(7) Component test** (mocked) | Each component honors its contract in isolation. |
| 5 | **Implementation**   | Production code (`${config.layout.source_dir}`), following `${config.clean_code.*}` | **(6) Unit test** | Each unit (function/class) is correct. |

Traceability is mandatory: every requirement ID must reach ≥1 acceptance test
(`config.quality_gates.traceability`). The workflow emits a matrix each increment.

### 3b. The composition hierarchy (made explicit, visible, and tested)

The five left-arm stages build a strict **composition hierarchy** (`config.v_model.composition`),
each tier *one or more* of the tier below, and each tier has its **own test level**:

```
   software system  ◄── (10) acceptance test  — run the whole system end-to-end
      ▲ one or more
   software (executable / deployable)  ◄── (9) system test  — deployables run together (topology)
      ▲ one or more
   module(s)        ◄── (8) module test (mocked)  — per module
      ▲ one or more
   component(s)     ◄── (7) component test (mocked)  — per component
      ▲ one or more
   unit(s)          ◄── (6) unit test  — per component's units
```

- **unit → component:** a unit belongs to **exactly one** component.
- **component → module:** a component may serve **one or more** modules — a shared component is
  **reused, implemented once**, not duplicated.
- **module → software (executable):** an executable is composed of its modules.
- **software → software system:** the system (its *topology*, e.g. client-server) is composed of its
  executables (e.g. a client and a server).

Making the hierarchy explicit buys three things: each element is **visible** (it appears in
`OUTPUT.md` and the per-phase traces), each element is **independently tested** at its level, and —
because siblings are independent — the work runs **highly in parallel** (§4).

### 3c. How the names map to standard terminology

The composition hierarchy isn't standardized identically across the industry (the same words —
*module*, *component*, *unit* — are used differently project to project), but Hephaestus's chain lines up
with widely-used definitions and the classic V-Model:

| Hephaestus term | Standard alignment |
|------------|--------------------|
| **unit** | ISTQB *unit/component test* target; ISO 26262 *software unit* (the lowest design piece). |
| **component** (one or more units) | ISO 26262: "a software component gathers one or more software units." |
| **module** (one or more components) | Common definition: "a module is a set of components with specific interfaces." |
| **software / executable** (one or more modules) | A deliverable build artifact — the *subsystem/application* level. |
| **software system** (one or more executables) | The top *system* element; its **topology** (e.g. client-server) is the arrangement of executables. |

Test-level mapping to the classic V-Model (ISTQB: *unit → integration → system → acceptance*): Hephaestus's
**unit** and **acceptance** match directly; the V-Model's **integration testing** is split by tier into
**component** (units within a component, mocked), **module** (components within a module, mocked) and
**system** (executables running together) tests; **system** and **acceptance** match the V-Model's
system and acceptance levels.

---

## 4. Forward decomposition, clear gates, targeted repair

The left arm flows **forward only** — Requirements → Software System → Architecture → (per-component)
Design → Implementation. There is **no backward jump** from one design stage to its predecessor: a
stage does its best with what it has, because any real gap will surface concretely in the **test
phases**. This keeps the decomposition simple and lets it run in parallel without speculative rework.

Each test level is a **clear gate** (a barrier): the climb to the next level proceeds only once the
current level is green. When a gate goes red, Hephaestus applies a **targeted repair** — it repeats the
build loop **only for the failing element** and re-verifies, bounded by `config.agile.max_fix_rounds`:

> This is **abstract and the same at every level**. If one unit fails, only that unit's
> red→green→refactor loop (plus refactoring) is repeated — the other units of the component are
> already green and are left untouched. The identical rule applies one tier up: if a component fails
> its (mocked) test, only that component is repaired; if a module fails, only that module's wiring; if
> a deployable fails the system test, only that deployable — never a wholesale re-implementation.

| Gate (test level) | Targeted repair — repeat the loop for… |
|-------------------|----------------------------------------|
| unit (6)        | the failing **unit(s)** |
| component (7)   | the failing **component(s)** (the units behind the contract failure) |
| module (8)      | the failing **module(s)** (the component wiring at that boundary) |
| system (9)      | the failing **deployable(s)** (how its modules assemble in the topology) |
| acceptance (10) | the specific behavior the scenario exercises, across the running system |

**Already-passing siblings are never re-implemented.** Only if a level stays red after
`config.agile.max_fix_rounds` does the climb stop and the increment fail its gate — then it re-loops
per `config.agile.max_gate_retries`, the coarse safety net. Test frameworks, sanitizers, and coverage
tools are whatever `config.toolchain.test_frameworks`, `config.toolchain.sanitizers`, and
`config.toolchain.coverage` name.

---

## 5. Quality gates (the exit criteria)

The **Iteration Gate** (step 6) blocks the increment until every threshold in
`config.quality_gates` is met. Defaults shipped in the config:

- Unit line coverage ≥ `unit_line_coverage_min`, branch ≥ `unit_branch_coverage_min`.
- Cyclomatic complexity ≤ `cyclomatic_complexity_max` per function.
- Linter findings (`clang-tidy`, `cppcheck`, …) = 0 (`fail_on_warning`).
- Formatter check `enforced`.
- Sanitizers clean.
- Public API documentation coverage = `public_api_doc_coverage_min`.
- Traceability matrix complete.

A failed gate re-loops the increment up to `config.agile.max_gate_retries` times;
persistent failure is surfaced, never silently passed.

---

## 6. Clean code & refactoring discipline

Refactoring is **on-demand inside every phase** (the *refactor* of red→green→refactor), not a
separate stage: whenever an agent spots a smell while working, it applies the matching technique
immediately and keeps tests green. The lenses it draws on, from `config.clean_code`:

- **Principles:** SOLID, DRY, KISS, YAGNI, Law of Demeter, composition > inheritance.
- **Architecture:** `config.clean_code.architecture` (default Ports & Adapters) with
  the **dependency rule** — source dependencies point inward toward the domain core,
  which has no I/O dependencies.
- **Smell hunt:** every smell in `config.clean_code.forbidden_smells` is searched for
  and removed while tests stay green.
- **Boy-Scout rule:** leave touched code cleaner than found; no new warnings.

The clean-code **thresholds** (complexity, function length, dependency rule, zero warnings) are
**enforced at the Iteration Gate**, so quality is gated even though refactoring itself is on-demand.

## 6b. Reference catalogs (authority sources)

Three stages cross-check their work against well-known external catalogs, listed
under `config.references` (URLs are configurable; canonical lists are embedded so
agents work offline):

| Stage | Catalog | Source | Rule |
|-------|---------|--------|------|
| **Architecture** | Software architecture patterns | `references.software_architecture_patterns` (tecnovy top-10) | Name the chosen pattern(s) and justify in an ADR. |
| **Design** | GoF design patterns (Creational/Structural/Behavioral) | `references.design_patterns` (refactoring.guru) | Justify every pattern used by name. |
| **Refactor** | Code smells + refactoring techniques | `references.refactoring` (refactoring.guru) | Name each detected smell and the technique applied to remove it. |
| **Docs** | arc42 12-section template | `references.documentation` (arc42) | Architecture docs follow the arc42 structure. |

Swapping an authority (e.g. a different pattern catalog) is a config edit — the
plan, process, and workflow read `config.references` by reference.

## 6c. Living artifacts — the memory of the cyclic loop

Every phase produces **persisted artifacts**, and those artifacts are deliberately
designed to serve **both the human and the agent** on the next turn of the cycle —
they are the project's shared memory, not throwaway by-products. See
`config.living_artifacts` and `config.carry_forward`.

| Phase | Artifact (lives in the repo) | Used next cycle by… |
|-------|------------------------------|---------------------|
| Requirements | `docs/requirements/<INC>.md` + a **traceability matrix that grows** | human: scope review · agent: regression awareness |
| Architecture | arc42 doc + **append-only ADRs** + UML | human: onboarding · agent: keeps decisions consistent |
| Design | `docs/design/<INC>.md` + component-test specs | both: contract reference |
| Implementation | code + tests (**executable specification**) | both: ground truth |
| Increment close | `docs/increments/<INC>-report.md` (gates, `smell→technique`, **debt log**) | human: steering · agent: next-increment input |

**Carry-forward rule:** each increment is *given* the prior ADRs, the current
traceability matrix, and the debt log as input context. Decisions and debt recorded
in one cycle become **requirements/constraints** for later cycles — so the loop
accumulates knowledge instead of repeating itself.

**Keep it minimal & effective.** Per `config.references.documentation.principle`
(arc42 lean subset): document decisions, interfaces and rationale — not the obvious.
Prefer a diagram to prose, mark irrelevant sections _n/a_, and treat redundant prose
as the *Comments* smell. The goal is documentation that is *read*, by a person or an
agent, on the next iteration.

**Documentation toggle.** `config.toggles.documentation` switches the whole
documentation effort: `full` (arc42 + API docs + UML, still lean), `minimal`
(ADRs + a building-block sketch only), or `off` (code and tests are the spec — the
"Public API documented" item is then dropped from the Definition of Done). The
workflow's Architecture and Gate phases honor this toggle automatically.

---

## 7. Retargeting to another language

Nothing above is C++-specific. To target Rust, Go, Python, TypeScript, …:

1. Edit `config/hephaestus.config.yaml`: `language.*`, `toolchain.*` (e.g. swap
   `clang-tidy`→`clippy`, `clang-format`→`rustfmt`, `GoogleTest`→`cargo test`,
   `cmake`→`cargo`).
2. Adjust `quality_gates` thresholds if the ecosystem differs.
3. Leave this plan, the process spec, and the workflow untouched — they read the
   config by reference.

---

## 8. How to run it

See [`../README.md`](../README.md) for the quickstart. In short:

- **Automated:** run the workflow at `.claude/workflows/hephaestus.js` (it
  orchestrates one agent per stage, per increment, with verification and gates).
- **Manual / any tool:** follow the step-by-step entry/exit criteria and prompt
  templates in [`hephaestus.process.md`](hephaestus.process.md).

Both consume the same config file, so automated and manual runs stay consistent.

### The `INPUT.md` / `OUTPUT.md` interface

You normally don't touch YAML. Write plain sentences in `config.interface.input`
(`INPUT.md`) — the language, design, tools, gates, and feature ideas — and the
**Intake** step (Stage 0) updates the project files to match: high-level choices are
written into `config/hephaestus.config.yaml`, feature ideas become backlog increments.
`INPUT.md` can be **super minimal** because the config carries defaults; you write only
what should differ. After each increment the **Report** step overwrites
`config.interface.output` (`OUTPUT.md`) with a short status checklist and re-reads
`INPUT.md` for anything new — so you can keep adding ideas while the loop runs.

### Per-phase model routing

`config.models` assigns a model tier (`opus`/`sonnet`/`haiku`) to each V-Model
phase, so judgment-heavy phases (architecture, design, gate) can run on a stronger
model than the mechanical, high-volume ones (verification, implementation). A phase
falls back to `models.default`; with no `models` block, every phase inherits the
session model. This is a Claude Code workflow capability; on the manual/Copilot path
it serves as a per-phase model recommendation.
