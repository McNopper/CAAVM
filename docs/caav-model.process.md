# CAAVM — Portable Process Specification

> Tool-agnostic, step-by-step procedure for running **one increment** of the
> Cyclic Agentic Agile V-Model. Follow this by hand, with any agent harness, or
> as the contract the automated workflow implements. Every `${config.x}` refers
> to [`../config/caav-model.config.yaml`](../config/caav-model.config.yaml).
>
> Repeat this whole procedure once per entry in `${config.project.backlog}`.
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
> and tests are the spec — skip Stage-2 arc42 docs and drop the doc item from the DoD).

---

## Interface: `INPUT.md` → process → `OUTPUT.md`

The human drives the whole loop through two markdown files at the project root
(`${config.interface.input}` / `${config.interface.output}`):

- **`INPUT.md` (you write, anytime — even mid-run).** Plain sentences: the language,
  design style, tools, gates, docs level, and feature ideas. It can be **super minimal** —
  the config already holds defaults, so you write only what should differ. A single
  sentence is a valid input.
- **Stage 0 — Intake (process).** Read `INPUT.md` and **update the project files to match**:
  map high-level choices into `${config.x}` (language, toolchain, quality_gates,
  toggles.documentation, models, …) and turn feature ideas into backlog increments. Record
  what was captured back in `INPUT.md`; never delete the human's prose.
- **`OUTPUT.md` (process writes).** After each increment, overwrite it with a short, structured
  status checklist (per-increment checkbox + stage + gate + the five test levels + debt + next
  action). The human reads state here; the loop re-checks `INPUT.md` for new ideas every cycle.

## Recommended model per phase

When your harness supports per-phase models (`${config.models}`), use a stronger tier for
judgment-heavy phases and a cheaper one for mechanical work. Shipped default:

| Phase | Recommended tier |
|-------|------------------|
| Intake | sonnet |
| Requirements | sonnet |
| Architecture | **opus** |
| Design | **opus** |
| Implementation (TDD) | sonnet |
| Verification | haiku |
| Refactor | sonnet |
| Iteration Gate | **opus** |
| Report | sonnet |

On a single-model harness (e.g. Copilot CLI / manual), treat this as guidance — switch model
manually for the opus phases if your tool allows, otherwise run the whole loop on one capable model.

---

## Roles (one responsibility each)

| Role | Owns | Hands off |
|------|------|-----------|
| Requirements Analyst | Stage 1 + acceptance test spec | to Architect |
| Architect            | Stage 2 + integration test plan | to Designer |
| Designer             | Stage 3 + component test specs | to Implementer |
| Implementer (TDD)    | Stage 4 code + unit tests | to Verifier |
| Verifier             | Runs unit→component→module→integration→acceptance | to Refactorer |
| Refactorer           | Clean-code pass | to Gatekeeper |
| Gatekeeper           | Quality gates + Definition of Done | next increment |

In the agentic version each role is a subagent; verification roles are run by
*different* agents than the ones that produced the work (adversarial check).

---

## Stage 1 — Software Requirements  →  Acceptance/System Test

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

## Stage 2 — Software Architecture  →  Module/Integration Test

**Entry:** approved requirements.
**Do:**
- **Pick a pattern from the catalog** `${config.references.software_architecture_patterns.catalog}`
  (Layered, Microservices, Event-Driven, Repository, CQRS, DDD, …) and justify it.
- Decompose into modules with clear boundaries; apply `${config.clean_code.architecture}`
  and the dependency rule (`${config.clean_code.dependency_rule}`).
- Define inter-module interface contracts.
- Record decisions as short ADRs (context → decision → consequences).
- **Document using the arc42 template** `${config.references.documentation.sections}`
  (Introduction & Goals, Context, Solution Strategy, Building Block View, Runtime View, …).
- Write the **integration/module test plan**: which module collaborations are
  exercised and how, using `${config.toolchain.test_frameworks.integration}`.
**Exit criteria:** a named architecture pattern is chosen; the domain core has no I/O
dependencies; each requirement maps to ≥1 module; arc42 doc drafted; integration test
plan covers every boundary.
**Prompt template:**
> You are the Architect. Given these requirements, choose an architecture pattern from
> `${config.references.software_architecture_patterns.catalog}` and justify it. Define
> modules, boundaries, and interface contracts honoring `${config.clean_code.architecture}`
> and the inward dependency rule. Document with the arc42 sections
> `${config.references.documentation.sections}`. Emit ADRs and an integration test plan;
> map each REQ to a module.

---

## Stage 3 — Software Design  →  Component Test

**Entry:** approved architecture.
**Do:**
- For each module, design its components: classes/functions, interfaces, data
  structures, applied design patterns **chosen from the catalog**
  `${config.references.design_patterns}` (Creational/Structural/Behavioral —
  Adapter, Strategy, Factory Method, Observer, …); justify each by the problem it
  solves — no pattern for its own sake (avoid the _Speculative Generality_ smell).
- Specify error handling per `${config.clean_code.error_handling}` and resource
  ownership per `${config.clean_code.resource_management}`.
- Write **component test specs** (contract-level, dependencies mocked with
  `${config.toolchain.test_frameworks.unit.mock}`).
**Exit criteria:** every public interface has a contract and a component test spec.
**Prompt template:**
> You are the Designer. For each module, specify components, public interfaces,
> data structures, and the design patterns used (with justification). Define error
> handling and ownership. Produce component-test specs.

---

## Stage 4 — Software Implementation  →  Unit Test (TDD)

**Entry:** approved design.
**Do, per unit (red→green→refactor):**
1. Write the failing unit test from the component contract
   (`${config.toolchain.test_frameworks.unit.tool}`).
2. Write the minimum `${config.language.name}` `${config.language.standard}` code to pass.
3. Tidy locally (naming, small functions) keeping the test green.
- Place code under `${config.layout.source_dir}` / `${config.layout.include_dir}`,
  tests under `${config.layout.test_dir}`.
- Run `${config.toolchain.formatter.fix_command}` and the linters as you go.
**Exit criteria:** all unit tests green; no linter/formatter findings on touched files.
**Prompt template:**
> You are the Implementer using TDD. Implement component `${c}` in
> `${config.language.name} ${config.language.standard}`. For each unit: write the
> failing test first, then minimal code. Obey `${config.clean_code.*}`. Run
> `${config.toolchain.formatter.tool}` and `${config.toolchain.linters[*].tool}`.

---

## Stage 5 — Verification (climb the V, bottom-up)

Run in the order `${config.v_model.test_execution_order}`:

1. **Unit tests** — fast, isolated, with `${config.toolchain.sanitizers}` enabled.
2. **Component tests** — each component against its contract, collaborators mocked.
3. **Module/Integration tests** — real module collaborations across boundaries.
4. **Software-Integration test** — the assembled software.
5. **Acceptance/System test** — the Stage-1 scenarios, end to end.

Collect coverage via `${config.toolchain.coverage.tool}`. Any red stops the climb
and returns to the owning stage. **A different agent than the implementer runs and
reviews these** (adversarial verification).

---

## Stage 6 — Refactor pass (bounded by `${config.agile.max_refactor_rounds}`)

With every test green, run review lenses and apply fixes while keeping tests green:
- **Principles lens:** `${config.clean_code.principles}`.
- **Smell lens:** hunt the catalog `${config.references.refactoring.smells}` (Bloaters,
  OO-Abusers, Change-Preventers, Dispensables, Couplers). For each smell found, name it
  and apply the matching **technique** from `${config.references.refactoring.techniques}`
  (Extract Method, Replace Conditional with Polymorphism, Move Method, …).
- **Architecture lens:** dependency rule intact? boundaries clean?
- **Naming/size lens:** functions ≤ `${config.quality_gates.function_length_max_lines}` lines,
  complexity ≤ `${config.quality_gates.cyclomatic_complexity_max}`.
Re-run Stage 5 after refactoring. Record each `smell → technique` change in the increment report.

---

## Stage 7 — Iteration Gate (Definition of Done)

Pass the increment only if **all** hold (else re-loop, bounded by
`${config.agile.max_gate_retries}`):

- [ ] All five test levels green.
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
