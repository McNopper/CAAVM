# Cyclic Agentic Agile V-Model (CAAVM) — Plan & Methodology

> A reusable, language-agnostic methodology for building software increment by
> increment, where **every design stage is paired with the test level that
> verifies it** (the V), the V is **repeated each iteration** (the cycle), and
> **autonomous agents** drive each stage under enforced **clean-code and
> refactoring** gates.
>
> All language/tool specifics live in [`../config/caav-model.config.yaml`](../config/caav-model.config.yaml).
> This document never hard-codes C++ — it reads `${config.*}` placeholders.

---

## 1. Why "Cyclic Agentic Agile V-Model"

| Word | What it contributes |
|------|---------------------|
| **Cyclic**  | The full V is re-run for *each* backlog increment, not once for the whole project. |
| **Agentic** | Each stage is executed by a dedicated agent with a single responsibility; verification is adversarial (separate agents try to break the work). |
| **Agile**   | Small vertical slices, working software every iteration, refactoring as a first-class step. |
| **V-Model** | Strict traceability: requirements ↔ acceptance, architecture ↔ integration, design ↔ component, implementation ↔ unit. |

The classic V-Model's weakness is that it is waterfall — one giant pass. CAAVM
keeps the V-Model's traceability discipline but **folds it into the agile loop**:
one short, complete V per increment.

---

## 2. The shape of one increment (one V-pass)

```
 LEFT ARM (decompose & design)                         RIGHT ARM (verify, bottom-up)
 ──────────────────────────────                        ────────────────────────────
                                                                   ┌────────────────┐
 (1) Software Requirements ───────────────────────────────────────► Acceptance /    │
        │  what & why, acceptance criteria                          │ System /        │
        │                                                           │ SW-Integration  │
        ▼                                                           │ Test  (final)   │
 (2) Software Architecture ──────────────────────────────►┌────────┴────────┐       │
        │  modules, boundaries, ADRs                       │ Module /         │       │
        │                                                  │ Integration Test │       │
        ▼                                                  └────────┬─────────┘       │
 (3) Software Design ─────────────────────────►┌──────────────────┐│                 │
        │  components, interfaces, patterns     │ Component Test    ││                 │
        │                                       └─────────┬─────────┘│                 │
        ▼                                                 │          │                 │
 (4) Software Implementation ─►┌──────────────┐           │          │                 │
        code (TDD)             │  Unit Test    │           │          │                 │
                               └──────┬────────┘           │          │                 │
                                      │                     │          │                 │
                                      ▼                     ▼          ▼                 ▼
                                    GREEN ───────────────► GREEN ───► GREEN ──────────► GREEN
                                      └──────────────► (5) REFACTOR pass ◄──────────────┘
                                                             │  clean code / SOLID / smells
                                                             ▼
                                                      (6) ITERATION GATE
                                                          quality gates + DoD
                                                          pass ▸ next increment
                                                          fail ▸ re-loop (bounded)
```

**Left arm** flows top-down; each stage refines the previous and *also designs the
test* that will later verify it. **Right arm** executes bottom-up: units first,
then components, then modules, then the final software-integration/acceptance
test — exactly the order in `config.v_model.test_execution_order`.

---

## 3. Stages, artifacts, and the test paired with each

| # | Left stage (design) | Primary artifacts | Paired test level (right) | Verifies |
|---|---------------------|-------------------|---------------------------|----------|
| 1 | **Requirements**    | User stories, acceptance criteria, non-functional reqs, traceability IDs | **Acceptance / System / final SW-Integration test** | The increment does what the user asked, end to end. |
| 2 | **Architecture**    | Module decomposition, boundaries, dependency rule, ADRs, interface contracts | **Module / Integration test** | Modules collaborate correctly across boundaries. |
| 3 | **Design**          | Component interfaces, data structures, applied patterns, sequence/flow | **Component test** | Each component honors its contract in isolation. |
| 4 | **Implementation**  | Production code (`${config.layout.source_dir}`), following `${config.clean_code.*}` | **Unit test** | Each unit (function/class) is correct. |

Traceability is mandatory: every requirement ID must reach ≥1 acceptance test
(`config.quality_gates.traceability`). The workflow emits a matrix each increment.

---

## 4. Test-Driven flow on the right arm

For each unit in the increment, agents follow **red → green → refactor**:

1. **Red** — write the failing test from the design's contract first.
2. **Green** — write the minimum production code to pass.
3. **Refactor** — clean it up (see §6) with tests staying green.

Then climb the V: component tests, module/integration tests, and finally the
acceptance test that closes the loop back to the requirement. Test frameworks,
sanitizers, and coverage tools are whatever `config.toolchain.test_frameworks`,
`config.toolchain.sanitizers`, and `config.toolchain.coverage` name.

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

Refactoring is a **named, mandatory step** every increment, not an afterthought.
The refactor pass runs adversarial review lenses drawn from `config.clean_code`:

- **Principles:** SOLID, DRY, KISS, YAGNI, Law of Demeter, composition > inheritance.
- **Architecture:** `config.clean_code.architecture` (default Ports & Adapters) with
  the **dependency rule** — source dependencies point inward toward the domain core,
  which has no I/O dependencies.
- **Smell hunt:** every smell in `config.clean_code.forbidden_smells` is searched for
  and removed while tests stay green.
- **Boy-Scout rule:** leave touched code cleaner than found; no new warnings.

Refactors are bounded by `config.agile.max_refactor_rounds` per increment so the
loop always converges.

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

1. Edit `config/caav-model.config.yaml`: `language.*`, `toolchain.*` (e.g. swap
   `clang-tidy`→`clippy`, `clang-format`→`rustfmt`, `GoogleTest`→`cargo test`,
   `cmake`→`cargo`).
2. Adjust `quality_gates` thresholds if the ecosystem differs.
3. Leave this plan, the process spec, and the workflow untouched — they read the
   config by reference.

---

## 8. How to run it

See [`../README.md`](../README.md) for the quickstart. In short:

- **Automated:** run the workflow at `.claude/workflows/caav-model.js` (it
  orchestrates one agent per stage, per increment, with verification and gates).
- **Manual / any tool:** follow the step-by-step entry/exit criteria and prompt
  templates in [`caav-model.process.md`](caav-model.process.md).

Both consume the same config file, so automated and manual runs stay consistent.
