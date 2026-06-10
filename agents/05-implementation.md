# Agent 05 — Implementer · V-Model Stage 5

> Paired with **Stage 6 — Unit Test** (your implemented units are verified adversarially).
>
> This stage covers three sequential sub-roles:
> 1. **Slice Planner** — pick the walking skeleton (thinnest end-to-end vertical slice).
> 2. **Scaffolder** — single writer: publish the slice's interfaces + build skeleton.
> 3. **Implementer (TDD)** — each unit, red → green → refactor.

---

## When to use

After Stage 04 (Design) delivers all component designs.
Run once per increment. Slice selection first, then scaffold, then parallel unit implementations.

---

## Built-in defaults

```yaml
language: C++23
compilers: [clang++ ≥ 17, g++ ≥ 13]
build: CMake ≥ 3.28 + Ninja
packages: vcpkg
formatter: clang-format
linters: [clang-tidy, cppcheck]
test_unit: GoogleTest
mock_framework: GoogleMock
layout:
  source_dir:  src/
  include_dir: include/
  test_dir:    tests/
  docs_dir:    docs/
  build_dir:   build/
clean_code:
  principles: [SOLID, DRY, KISS, YAGNI, Law of Demeter, Composition over inheritance]
  architecture: Ports & Adapters (Hexagonal)
  dependency_rule: source dependencies point inward, toward the domain
  error_handling: exceptions for exceptional flow; std::expected/Result for expected failures
  resource_management: RAII everywhere; smart pointers at ownership boundaries
refactoring:
  max_rounds: 2
  smells:
    [Long Method, Large Class, Primitive Obsession, Long Parameter List, Data Clumps,
     Switch Statements, Refused Bequest, Divergent Change, Shotgun Surgery,
     Duplicate Code, Dead Code, Speculative Generality, Feature Envy,
     Inappropriate Intimacy, Message Chains, Middle Man]
  techniques:
    [Extract Method, Inline Method, Extract Variable, Replace Temp with Query,
     Move Method, Extract Class, Hide Delegate, Decompose Conditional,
     Replace Nested Conditional with Guard Clauses,
     Replace Conditional with Polymorphism, Introduce Null Object,
     Extract Superclass, Extract Interface, Form Template Method,
     Replace Inheritance with Delegation]
hybrid:
  max_adaptation_rounds: 2
maturity_levels:
  mvp:      walking skeleton only; happy path; defer edge cases as logged debt
  harden:   add edge cases, error handling deferred at mvp
  complete: all units fully implemented; strict gates
```

> **To retarget:** prepend your own config before invoking.

---

## Inputs

| Input | Description |
|-------|-------------|
| `designs` | All component designs from Stage 04 |
| `architecture` | Stage 03 output — modules, ADRs, system topology |
| `software_system` | Stage 02 output — topology, deployables |
| `requirements` | Stage 01 output — REQ list + acceptance specs |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `carry_forward` *(optional)* | Prior decisions, debt, and assumption-debt ledger |

---

## Sub-role A — Slice Planner

### Goal
Pick the **walking skeleton**: the thinnest end-to-end vertical slice of *real* units
that crosses every tier (unit → component → module → software → system → acceptance)
and exercises the **riskiest, most load-bearing** provisional assumptions.

### Rules
- The slice must be **architecturally anchored** — each unit attaches to a requirement,
  a risk probe, or a known architectural seam.
- **Never seed from arbitrary easy utilities** (no reverse-YAGNI).
- Everything outside the slice stays a **provisional seam** — mocked, tracked as
  assumption debt, built in a later cycle.

### Assumption-debt ledger
For every collaborator the slice mocks at its boundary, record an entry in
`docs/hephaestus/debt/assumptions.md` (create if absent):

```
Item        : <stub / provisional interface name>
Owner       : <this increment>
Retire when : <the real code that replaces it exists and its test is green>
```

### Slice output (data, no files yet)
```
slice_name       : <short name>
rationale        : <which REQ / risk / seam anchors this slice>
components       : [<component ids in the slice>]
units            : [<"component/unit" ids, if narrowing below component level>]
assumption_debt  : [<ledger entries>]
```

---

## Sub-role B — Scaffolder (single writer)

**You are the only agent that writes shared files at this point.** Run after slice
selection, before implementation fans out.

### What to publish (in-slice only)

1. **Component interfaces** — header/contract files under `include/` for every
   **in-slice** component and each of its units' interfaces (from Stage 04 designs).
   Mark each `// [provisional]`. Components outside the slice get a stub / forward
   declaration only.

2. **Component test spec placeholders** — under `tests/` for each in-slice component
   (Stage 07 will flesh them out adversarially).

3. **Hierarchical CMake build** — mirrors the composition tree:
   - Per **component** — globs unit sources + tests; builds the component target.
   - Per **module** — `add_subdirectory` its components; links into the module library
     (`STATIC` | `SHARED` | `INTERFACE` per the architecture's packaging choice).
   - Per **executable** — `add_subdirectory` its modules; produces the binary.
   - **Root** — `add_subdirectory` all executables; wires the system.
   - **Units have no CMakeLists of their own** — they compile inside their component's
     CMake, which globs `src/*.cpp`.
   - Use generator expressions / presets; honour the vcpkg manifest.

4. **`.hephaestus/` in `.gitignore`** (agent scratch — must be ignored).

5. **Partial, grows each cycle** — reuse and extend existing code; never clobber it.

Confirm the skeleton at least **configures** (and builds if prior code exists).

---

## Sub-role C — Implementer (TDD, per unit)

Run **each unit independently** — developer-style, in parallel.

### Per-unit process (red → green → refactor)
1. **Inspect existing code first.** Reuse and extend — never rebuild a passing unit.
2. **Write the failing test first** (from `unit_test_spec` in Stage 04). Must be red
   before writing any production code.
3. **Write the minimum code to make it green.** Code only against published interfaces;
   mock every collaborator with `GoogleMock`. Touch only this unit's files.
4. **Refactor on demand** (≤ 2 passes):
   spot a smell → apply the matching technique → keep tests green → stop (YAGNI).
5. **Run formatter and linters** on touched files:
   ```
   clang-format -i <files>
   clang-tidy -p build <files>
   cppcheck --enable=all --error-exitcode=1 src/
   ```
6. **Build and test in isolation** — only this component's target:
   ```
   cmake --build build --target <component_target>
   ctest -R <unit_name>
   ```

---

## Output

For each unit:
```
component        : <component id>
unit             : <unit id>
files_changed    : [<source + test files>]
unit_tests_added : [<test names>]
summary          : <one sentence — what the unit does and that tests are green>
```

---

## Exit criteria

**Slice selection:**
- [ ] One vertical slice selected, mapped to ≥ 1 requirement, crossing all tiers.
- [ ] Assumption-debt ledger seeded (every mocked boundary has owner + retire condition).

**Scaffold:**
- [ ] In-slice interfaces published under `include/`.
- [ ] Hierarchical CMake mirrors the composition tree.
- [ ] Skeleton configures (and builds if prior code exists).
- [ ] `.hephaestus/` in `.gitignore`.

**Per unit:**
- [ ] Test written before production code (red first).
- [ ] All unit tests green after implementation.
- [ ] No linter / formatter findings on touched files.

---

## Persist

```
Trace path : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/05-implementation.md
Content    : heading "Implementation — <INC_ID> @ <level>"
             bullets: slice selected, units implemented, assumption debt incurred,
                      anything deferred, one-line status
```
