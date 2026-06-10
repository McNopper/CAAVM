---
name: 04-design
description: Hephaestus V-Model Stage 4. Detail each component's public interface, units, design patterns, and provisional component test spec. Returns data only, writes no files. Use after architecture delivers the component work-list. Paired with Stage 7 component testing.
---

# Skill 04 — Designer · V-Model Stage 4

> Paired with **Stage 7 — Component Test** (your component interfaces are the mocked
> contracts that Stage 07 verifies).
>
> **Role:** detail each component from the architecture's work-list — define its public
> interface, the units that compose it, the design patterns applied, and the component
> test specification. **Return data only — write no files.**
> The Scaffold step inside Stage 05 is the single writer.
> All contracts are **PROVISIONAL**.

---

## When to use

After Stage 03 (Architecture) delivers the component work-list.
Run all components **in parallel** — each component is independent at this stage.
After all are designed, hand the full set to Stage 05 (Implementation).

---

## Built-in defaults

```yaml
language: C++23
compilers: [clang++ ≥ 17, g++ ≥ 13]
test_unit:      GoogleTest
test_component: GoogleTest
mock_framework: GoogleMock
clean_code:
  principles: [SOLID, DRY, KISS, YAGNI, Law of Demeter, Composition over inheritance]
  architecture: Ports & Adapters (Hexagonal)
  dependency_rule: source dependencies point inward, toward the domain
  error_handling: exceptions for exceptional flow; std::expected/Result for expected failures
  resource_management: RAII everywhere; smart pointers at ownership boundaries
  forbidden_smells:
    [god class, long parameter list, primitive obsession, feature envy,
     shotgun surgery, duplicated logic, deep nesting (> 3 levels)]
design_patterns:
  creational:  [Factory Method, Abstract Factory, Builder, Prototype, Singleton]
  structural:  [Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy]
  behavioral:  [Chain of Responsibility, Command, Iterator, Mediator, Memento,
                Observer, State, Strategy, Template Method, Visitor]
docs:  toggle: minimal
maturity_levels:
  mvp:      minimal interface; YAGNI strictly; no over-engineering
  harden:   add error paths, edge-case units, tighten contracts
  complete: fully specified; strict gates
```

> **To retarget:** prepend your own config before invoking.
> Example: `Language: Java 21 · Test: JUnit 5 · Mock: Mockito`

---

## Inputs

| Input | Description |
|-------|-------------|
| `component_plan` | One entry from Stage 03's work-list: `{ name, modules[], responsibility }` |
| `architecture` | Stage 03 output — modules, boundaries, ADRs (for context) |
| `maturity_level` | `mvp` \| `harden` \| `complete` |

Invoke this skill **once per component** in the work-list. Run all invocations in parallel.

---

## Process

### 1 · Design the component interface (interface-first)
Define the public interface — the contract that decouples parallel work:
- What the implementer codes **against** and what collaborators **mock**.
- At `mvp`: minimal surface — only the operations required for the walking skeleton.
- Mark the interface **`[provisional]`**.
- Write it as a header/signature block (C++ class declaration, Rust trait, Go interface…),
  not a prose description.

### 2 · List the units
A unit is the smallest independently-testable piece inside this component.
A unit belongs to **exactly one** component.
For each unit:
- **name**, **interface** (unit's own signature/contract), **unit_test_spec** (1–2 sentences
  describing what the unit test must prove).

At `mvp` list only the units needed for the walking skeleton.

### 3 · Assign modules
Record **modules[]** this component serves (from Stage 03's work-list — one or more).
A component may serve multiple modules — list all; it is implemented once, not duplicated.

### 4 · Choose design patterns
Select only from the catalog above. For each:
- Name the pattern. Justify it in ≤ 1 sentence (the problem it solves).
- Apply **YAGNI**: if no pattern is needed, say so. Never apply a pattern speculatively.

### 5 · Specify error handling and resource ownership
- Error handling: `std::expected`/`Result` for expected failures; exceptions for truly
  exceptional conditions.
- Resource ownership: RAII everywhere; smart pointers at ownership boundaries.
- State these explicitly in the interface contract.

### 6 · Write the component test specification
Describe how this component will be tested at Stage 07:
- All collaborators mocked with `GoogleMock` (or configured mock framework).
- 2–4 test cases that verify the component's contract at its boundary.

---

## Output

**Return as data — do not write any files.
The Scaffold step in Stage 05 publishes everything.**

```
name               : <component id>
modules            : [<module ids>]
interface          : |
  // [provisional]
  <header / signature block in target language>
patterns           :
  - pattern        : <name>
    justification  : <one sentence>
units              :
  - name           : <unit id>
    interface      : <unit signature/contract>
    unit_test_spec : <what the unit test must prove>
component_test_spec: |
  Test cases (collaborators mocked):
  1. <input → expected output/behaviour>
  2. …
```

---

## Exit criteria

- [ ] Component has a named public interface tagged `[provisional]`.
- [ ] Every unit listed with its own interface and `unit_test_spec`.
- [ ] Every unit belongs to exactly this component.
- [ ] `modules[]` populated (one or more).
- [ ] Design patterns justified or explicitly omitted (YAGNI noted).
- [ ] Error handling and resource ownership stated.
- [ ] Component test spec has ≥ 2 test cases.
- [ ] **No files written** — data returned only.

---

## Persist

Stage 05 (Scaffold) writes files on behalf of all components together.
This agent does **not** write a trace file.
