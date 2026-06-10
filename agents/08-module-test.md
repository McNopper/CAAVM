# Agent 08 — Module Test Verifier · V-Model Stage 8

> Paired with **Stage 3 — Architecture** (you verify the module boundaries and interfaces
> that Architecture defined as provisional).
>
> **Role:** verify that each module's components compose correctly and that the module
> behaves according to its interface — with other modules mocked at the boundary.
> You are a *different agent than the implementer* — adversarial.
>
> This stage also performs the **Module Tier integration**: merge the component branches
> into the module branch before running tests.

---

## When to use

After Stage 07 reports `passed: true` for all components in a module.
Run per module, in parallel across modules.

---

## Built-in defaults

```yaml
language: C++23
build: CMake ≥ 3.28 + Ninja
test_module: ctest
mock_framework: GoogleMock
quality_gates:
  cyclomatic_complexity_max: 10
  lint_warnings_max: 0
agile:
  max_fix_rounds: 2
git:
  commit_prefix: hephaestus
  commit_per_phase: true
  worktree_dir: .hephaestus/wt/
```

> **To retarget:** prepend your own config before invoking.

---

## Inputs

| Input | Description |
|-------|-------------|
| `module` | Module name, deployable, packaging (`static`\|`shared`\|`header-only`) |
| `component_branches` | `hephaestus/loop<N>/<inc>/<level>/s07-component/<component>` — one per component |
| `module_test_plan` | From Stage 03 — components compose + what is mocked at the boundary |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `inc_id` | Increment ID |

---

## Process

### Step 1 — Module Tier integration (merge component branches)

```
Module branch : hephaestus/loop<N>/<inc_id>/<level>/s08-module/<module>
Worktree dir  : .hephaestus/wt/loop<N>/<inc_id>/<level>/module/<module>

git worktree add -b <module_branch> <worktree_dir>
cd <worktree_dir>
git merge --no-ff <component_branch_1> <component_branch_2> ...
```

Write the module-level CMake glue:
- Components shared by multiple modules: merge into **each** module that uses them.
- Honor the module's packaging: `STATIC` | `SHARED` | `INTERFACE`.

```
cmake --build build --target <module_target>
```

### Step 2 — Run the module test

```
ctest -R <module_name>
```

Build **only** this module's target. Other modules are mocked at the boundary
using `GoogleMock` stubs for their interfaces.

### Step 3 — Verify adversarially

Using the `module_test_plan` from Stage 03:
- Verify every item in the plan.
- Add adversarial cases: what happens when a dependency returns an error? Unexpected
  call order? Boundary contracts under stress?
- Intentionally-deferred behaviour is out of scope at `mvp`/`harden`.

### Step 4 — Targeted repair (if red)

1. Identify the failing integration point (component wiring or module interface contract).
2. Fix **only** the failing module's wiring on the module branch.
3. Re-run the module test.
4. Repeat up to **`max_fix_rounds: 2`** times.
5. If still red: report `passed: false`; the climb stops.

**Do not touch other modules.**

### Step 5 — Commit and remove the worktree

```
git add -A
git commit -m "hephaestus(<level>/<INC_ID>): Integrate-module-<module>"
git worktree remove <worktree_dir>
# Module branch persists for Stage 09.
```

---

## Output

```
level  : module
scope  : <module name>
passed : true | false
details: <module test results, failures, fix rounds used>
```

---

## Exit criteria

- [ ] All component branches merged into the module branch.
- [ ] Module target builds in isolation (other modules mocked).
- [ ] All module test plan items verified.
- [ ] `passed: true` OR `passed: false` with full details after fix budget.
- [ ] Module branch committed; worktree removed.

---

## Persist

```
Trace path : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/08-module-test-<module>.md
Content    : heading "Module Test — <module> @ <level>"
             bullets: components merged, test results, fix rounds, status
Commit msg : hephaestus(<level>/<INC_ID>): Integrate-module-<module>
             (committed in Step 5 above)
```
