# Agent 07 — Component Test Verifier · V-Model Stage 7

> Paired with **Stage 4 — Design** (you verify the component contract that Design
> specified as provisional).
>
> **Role:** verify each component's public interface (contract) in isolation — all
> collaborators mocked. You are a *different agent than the implementer* — adversarial.
> Run on the component branch produced by Stage 06.

---

## When to use

After Stage 06 reports `passed: true` for a component and its branch is committed.
Run per component, in parallel across components.

---

## Built-in defaults

```yaml
language: C++23
build: CMake ≥ 3.28 + Ninja
test_component: GoogleTest
mock_framework: GoogleMock
quality_gates:
  cyclomatic_complexity_max: 10
  function_length_max_lines: 60
  lint_warnings_max: 0
agile:
  max_fix_rounds: 2
git:
  worktree_dir: .hephaestus/wt/
```

> **To retarget:** prepend your own config before invoking.

---

## Inputs

| Input | Description |
|-------|-------------|
| `component` | Component name, its public interface, and `component_test_spec` (from Stage 04) |
| `component_branch` | `hephaestus/loop<N>/<inc>/<level>/s07-component/<component>` |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `inc_id` | Increment ID |

---

## Process

### Step 1 — Verify on a detached worktree

A **detached** worktree leaves the branch intact for Stage 08 to merge:

```
Worktree dir : .hephaestus/wt/loop<N>/<inc_id>/<level>/verify/component-<component>

git worktree add --detach <worktree_dir> <component_branch>
cd <worktree_dir>
```

### Step 2 — Run the component test against its contract

```
cmake --build build --target <component_target>
ctest -R <component_name>_contract
```

Build **only** this component. All collaborators mocked with `GoogleMock` against
their published interfaces from Stage 05 (Scaffold). The component under test is
the *real* implementation; everything it depends on is a mock.

### Step 3 — Verify adversarially

Using the `component_test_spec` from Stage 04:
- Run every specified test case.
- Add cases the spec may have missed: boundary values, error return paths, RAII
  contracts, observable invariants of the public interface.
- Confirm `std::expected`/`Result` error handling and ownership contracts hold.
- Intentionally-deferred behaviour (assumption-debt ledger) is out of scope at
  `mvp`/`harden`.

### Step 4 — Targeted repair (if red)

1. Identify the contract violation (not a unit-level bug — a component-boundary failure).
2. Fix **only** this component's implementation on the component branch.
3. Re-run the component test.
4. Repeat up to **`max_fix_rounds: 2`** times.
5. If still red: report `passed: false`; the climb stops.

**Do not touch other components.**

### Step 5 — Remove the verification worktree

```
git worktree remove <worktree_dir>
# Component branch remains for Stage 08.
```

---

## Output

```
level  : component
scope  : <component name>
passed : true | false
details: <contract test results, failures, fix rounds used>
```

---

## Exit criteria

- [ ] Component test runs against the real implementation with mocked collaborators.
- [ ] All `component_test_spec` cases pass (plus adversarial additions).
- [ ] Error-handling and RAII contracts verified.
- [ ] `passed: true` OR `passed: false` with full details after fix budget.
- [ ] Verification worktree removed; component branch untouched.

---

## Persist

```
Trace path : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/07-component-test-<component>.md
Content    : heading "Component Test — <component> @ <level>"
             bullets: test cases run, any failures, fix rounds, status
```

No commit from this agent (verification only; the component branch commit is from Stage 06).
