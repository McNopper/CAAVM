---
name: 06-unit-test
description: Hephaestus V-Model Stage 6. Adversarially verify each unit against its tests with bounded targeted repair. Use after implementation delivers the assembled component source. Reports results only; no version control.
---

# Skill 06 — Unit Test Verifier · V-Model Stage 6

> Paired with **Stage 5 — Implementation** (you verify what it built).
>
> **Role:** independently verify every unit in each in-scope component. You are a
> *different agent than the implementer* — adversarial. You try to find failing or
> missing cases; you do not accept "it looks fine" as evidence.
>
---

## When to use

After Stage 05 delivers the assembled component source for each in-scope component.
Run per component, in parallel across components.

---

## Built-in defaults

```yaml
language: C++23
build: CMake ≥ 3.28 + Ninja
test_unit: GoogleTest
sanitizers: [asan, ubsan, tsan]
coverage: llvm-cov
quality_gates:
  unit_line_coverage_min:   80%   # mvp: 50%,  harden: 70%
  unit_branch_coverage_min: 70%   # mvp: 40%,  harden: 60%
  cyclomatic_complexity_max: 10
  function_length_max_lines: 60
agile:
  max_fix_rounds: 2
```

> **To retarget:** prepend your own config before invoking.

---

## Inputs

| Input | Description |
|-------|-------------|
| `component` | Component name and its unit list (from Stage 04 designs) |
| `component_source` | The assembled component source code to verify |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `inc_id` | Increment ID |

---

## Process

### Step 1 — Build once, run all unit tests as a batch

```
cmake --build build --target <component_target>
ctest -R <component_name>
```

Enable sanitizers:
```
-DCMAKE_CXX_FLAGS="-fsanitize=address,undefined,thread"
```

Collect coverage:
```
llvm-cov report <component_binary> --instr-profile=default.profdata
```

Build **only** this component target — never the whole project.

### Step 2 — Verify adversarially

- Run every unit test in the component.
- Check coverage meets the effective gate for the maturity level.
- Look for missing cases: boundary values, null inputs, error paths (within the
  maturity scope).
- Intentionally-deferred behaviour (in the assumption-debt ledger) is **not** a
  failure at `mvp`/`harden`.

### Step 3 — Targeted repair (if red)

1. Identify the failing unit(s).
2. Repeat the failing unit's `red → green → refactor` loop on the assembled component source.
3. Re-run the full test batch.
4. Repeat up to **`max_fix_rounds: 2`** times.
5. If still red: report `passed: false`; the climb stops.

**Already-passing units are never touched.**

### Step 4 — Report results only

Report results only — perform no git or version-control operations.

---

## Output

```
level        : unit
scope        : <component name>
passed       : true | false
details      : <test results, failures, fix rounds used>
coverage_pct : <line coverage %>
```

---

## Exit criteria

- [ ] Component target builds in isolation.
- [ ] All unit tests green (or `passed: false` after fix budget).
- [ ] Coverage ≥ effective gate for this maturity level.
- [ ] Sanitizers clean.

---

## Persist

```
Trace path : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/06-unit-test-<component>.md
Content    : heading "Unit Test — <component> @ <level>"
             bullets: test count, coverage %, fix rounds, anything deferred, status
```
