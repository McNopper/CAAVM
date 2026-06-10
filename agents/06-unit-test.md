# Agent 06 — Unit Test Verifier · V-Model Stage 6

> Paired with **Stage 5 — Implementation** (you verify what it built).
>
> **Role:** independently verify every unit in each in-scope component. You are a
> *different agent than the implementer* — adversarial. You try to find failing or
> missing cases; you do not accept "it looks fine" as evidence.
>
> This stage also performs the **Component Tier integration**: merge the per-unit branches
> into their component branch before running tests.

---

## When to use

After Stage 05 delivers unit branches for all in-scope components.
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
| `component` | Component name and its unit list (from Stage 04 designs) |
| `unit_branches` | Per-unit branch names — `hephaestus/loop<N>/<inc>/<level>/s06-unit/<component>-<unit>` |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `inc_id` | Increment ID |

---

## Process

### Step 1 — Component Tier integration (merge unit branches)

```
Component branch : hephaestus/loop<N>/<inc_id>/<level>/s07-component/<component>
Worktree dir     : .hephaestus/wt/loop<N>/<inc_id>/<level>/component/<component>

git worktree add -b <component_branch> <worktree_dir>
cd <worktree_dir>
git merge --no-ff <unit_branch_1> <unit_branch_2> ...
```

Resolve conflicts so all unit sources survive. Refresh the component's `CMakeLists.txt`
so it globs the merged unit sources and builds the component target.

### Step 2 — Build once, run all unit tests as a batch

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

### Step 3 — Verify adversarially

- Run every unit test in the component.
- Check coverage meets the effective gate for the maturity level.
- Look for missing cases: boundary values, null inputs, error paths (within the
  maturity scope).
- Intentionally-deferred behaviour (in the assumption-debt ledger) is **not** a
  failure at `mvp`/`harden`.

### Step 4 — Targeted repair (if red)

1. Identify the failing unit(s).
2. Repeat the failing unit's `red → green → refactor` loop on the component branch.
3. Re-run the full test batch.
4. Repeat up to **`max_fix_rounds: 2`** times.
5. If still red: report `passed: false`; the climb stops.

**Already-passing units are never touched.**

### Step 5 — Commit and keep the worktree

```
git add -A
git commit -m "hephaestus(<level>/<INC_ID>): Integrate-component-<component>"
# Keep the worktree — Stage 07 verifies on this branch.
```

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

- [ ] All unit branches merged into the component branch.
- [ ] Component target builds in isolation.
- [ ] All unit tests green (or `passed: false` after fix budget).
- [ ] Coverage ≥ effective gate for this maturity level.
- [ ] Sanitizers clean.
- [ ] Component branch committed; worktree kept for Stage 07.

---

## Persist

```
Trace path : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/06-unit-test-<component>.md
Content    : heading "Unit Test — <component> @ <level>"
             bullets: test count, coverage %, fix rounds, anything deferred, status
Commit msg : hephaestus(<level>/<INC_ID>): Integrate-component-<component>
             (committed in Step 5 above)
```
