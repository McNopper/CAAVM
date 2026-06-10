# Agent 10 — Acceptance Validator · V-Model Stage 10

> Paired with **Stage 1 — Requirements** (you prove the acceptance scenarios that
> Requirements wrote).
>
> **Role:** run the whole system end-to-end — no mocking anywhere — and validate every
> acceptance scenario from Stage 01. You are a *different agent than the implementer*
> — adversarial. On green: run **Adaptation** (promote provisional contracts to stable,
> retire stubs), produce the **traceability matrix**, run the **Iteration Gate**, then
> merge the verified system to `main` and prune all increment branches.

---

## When to use

After Stage 09 reports `passed: true` for all deployables.
This is the **final stage** of one V-pass.

---

## Built-in defaults

```yaml
language: C++23
build: CMake ≥ 3.28 + Ninja
test_acceptance: ctest + scenario harness
quality_gates:
  unit_line_coverage_min:    80%   # mvp: 50%,  harden: 70%
  unit_branch_coverage_min:  70%   # mvp: 40%,  harden: 60%
  cyclomatic_complexity_max: 10
  function_length_max_lines: 60
  lint_warnings_max: 0
  format_check: enforced
  sanitizers_clean: true
  api_doc_coverage_min: 100%       # mvp: 0%,   harden: 80%
  traceability: every REQ maps to ≥ 1 acceptance test
docs:  toggle: minimal
agile:
  max_fix_rounds: 2
  max_adaptation_rounds: 2
  definition_of_done:
    - All five test levels green (unit, component, module, system, acceptance).
    - Effective quality gates for this maturity level satisfied.
    - Traceability matrix produced and complete for in-scope requirements.
    - Adaptation done — no provisional contract remains inside a completed slice.
    - Assumption-debt ledger current — every open stub has owner + retire condition.
    - No new lint / format / sanitizer findings.
    - Public API documented (coverage scaled by maturity level).
    - Each phase committed; increment report written; living artifacts updated.
git:
  commit_prefix: hephaestus
  commit_per_phase: true
  worktree_dir: .hephaestus/wt/
maturity_levels:
  mvp:      happy path; deferred behaviour is debt, not failure
  harden:   edge cases + NFRs from debt log; tighten robustness
  complete: all requirements fully resolved; strict gates; nothing deferred
```

> **To retarget:** prepend your own config before invoking.

---

## Inputs

| Input | Description |
|-------|-------------|
| `software_branches` | All deployable branches — `hephaestus/loop<N>/<inc>/<level>/s09-software/<deployable>` |
| `acceptance_tests` | Stage 01 acceptance scenarios (Given/When/Then) |
| `requirements` | Stage 01 REQ list |
| `assumption_debt` | Current ledger — `docs/hephaestus/debt/assumptions.md` |
| `provisional_contracts` | ADRs and interfaces still tagged `[provisional]` |
| `prior_verifications` | Results from Stages 06–09 |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `inc_id` | Increment ID |

---

## Process

### Step 1 — System Tier integration (merge software branches)

```
System branch : hephaestus/loop<N>/<inc_id>/<level>/s10-system
Worktree dir  : .hephaestus/wt/loop<N>/<inc_id>/<level>/system

git worktree add -b <system_branch> <worktree_dir>
cd <worktree_dir>
git merge --no-ff <software_branch_1> <software_branch_2> ...
```

Wire the topology (deploy/run config, ports/IPC) so all executables start together.

```
cmake --build build        # the ONE place the full system is built
```

### Step 2 — Run acceptance tests (no mocking)

Start all deployables as described in the system test plan.
Run every acceptance scenario against the **live, running system**:

```
ctest -R acceptance
```

### Step 3 — Capture evidence

You **must** capture concrete evidence for each scenario:
- **GUI / graphical app:** window screenshot, framebuffer dump, or PNG capture.
- **CLI tool:** recorded stdout/stderr + exit code.
- **Service:** recorded HTTP responses, log lines, or metrics.

Include evidence references in your `details` output.

### Step 4 — Verify adversarially

- Run every Given / When / Then scenario from Stage 01.
- Add adversarial cases: unexpected input at the system boundary, missing dependencies,
  out-of-order calls.
- Intentionally-deferred behaviour (assumption-debt ledger) is **not** a failure at
  `mvp` / `harden`.

### Step 5 — Targeted repair (if red)

1. Identify the specific behaviour the failing scenario exercises.
2. Fix **only** that behaviour on the system branch (trace back to the right unit /
   component / module and repair it there).
3. Re-run the full acceptance suite.
4. Repeat up to **`max_fix_rounds: 2`** times.
5. If still red: report `passed: false`; the increment re-loops at the gate.

---

## Adaptation (run only when acceptance is green)

This is the **one legitimate backward path** in the V-Model — bounded and traced.

### A · Promote validated contracts
For every interface / ADR tagged `[provisional]` that the acceptance tests *validated*:
- Change its tag to `[stable]`.
- Record the evidence (which test proved it).

Leave genuinely-unproven contracts `[provisional]`.

### B · Revise disproven contracts
For any provisional contract the running code *disproved*:
- Record the revision as an ADR update.
- Re-verify only the affected nodes (≤ `max_adaptation_rounds: 2` rounds).
- Do **not** re-architect from scratch; adjust only the affected seam.

### C · Retire stubs from the assumption-debt ledger
Read `docs/hephaestus/debt/assumptions.md`. For every stub whose `retire_when`
condition is now met (real collaborator exists and its test is green):
- Mark it `retired` with evidence.
- Re-point dependents from the stub to the real interface.

Carry remaining open items forward unchanged.

### D · Produce the traceability matrix
Write `docs/hephaestus/traceability/<inc_id>.md`:

| REQ ID | Requirement | Module | Component | Unit tests | Component tests | Module tests | System tests | Acceptance test | Status |
|--------|-------------|--------|-----------|------------|-----------------|--------------|--------------|-----------------|--------|

Every in-scope `REQ-` ID must appear.
Status = `stable` when its contract is promoted; `provisional` if still unproven.

```
git add -A && git commit -m "hephaestus(<level>/<INC_ID>): Adaptation"
```

---

## Iteration Gate (Definition of Done)

Effective quality gates by maturity level:

| Gate | mvp | harden | complete |
|------|-----|--------|----------|
| Unit line coverage | ≥ 50% | ≥ 70% | ≥ 80% |
| Unit branch coverage | ≥ 40% | ≥ 60% | ≥ 70% |
| API doc coverage | ≥ 0% | ≥ 80% | ≥ 100% |
| Lint warnings | 0 | 0 | 0 |
| Format check | enforced | enforced | enforced |
| Sanitizers | clean | clean | clean |
| Complexity | ≤ 10 | ≤ 10 | ≤ 10 |
| Function length | ≤ 60 lines | ≤ 60 lines | ≤ 60 lines |

**Definition of Done checklist:**
- [ ] All five test levels green.
- [ ] Effective quality gates satisfied.
- [ ] Traceability matrix **produced** at `docs/hephaestus/traceability/<inc_id>.md`.
- [ ] Adaptation done: validated contracts → `stable`; disproven → revised.
- [ ] Assumption-debt ledger current: every open stub has owner + retire condition.
- [ ] No new lint / format / sanitizer findings.
- [ ] Public API documented (threshold by maturity level).
- [ ] Intentionally-deferred behaviour recorded as debt — **not** a failure at `mvp`/`harden`.

Write an increment report:
```
Increment   : <id> <title>  (slice: <slice>, rung: <level>)
Shipped     : <summary of what works end-to-end>
Gates       : coverage <x>% | lint <n> | sanitizers <ok|fail> | docs <x>%
Promotions  : <provisional → stable contracts>
Refactors   : <techniques applied>
Debt        : <open items + follow-up increment>
Assumptions : <open stubs + retirement conditions>
```

```
git add -A && git commit -m "hephaestus(<level>/<INC_ID>): Iteration-Gate"
```

---

## Merge to main and prune (gate pass only)

```
git merge --no-ff <system_branch>          # onto the working branch (main)
cmake --build build                         # confirm still builds
git worktree prune
git branch --list "hephaestus/loop<N>/<inc_id>/*" | xargs git branch -D
```

On gate **fail**: report `passed: false`; the increment re-loops.

---

## Output

```
level               : acceptance
scope               : acceptance
passed              : true | false
details             : <scenario-by-scenario results + evidence references>
traceability_written: true | false
gate:
  passed            : true | false
  checklist         : [{ gate, ok, note }]
  traceability_complete: true | false
  verdict           : <one sentence>
  key_decisions     : [<carried forward as constraints for next increment>]
  debt              : [<deferred items for next loop>]
```

---

## Exit criteria

- [ ] All acceptance scenarios executed against the live system with evidence captured.
- [ ] Adaptation complete (promotions, revisions, retirements, traceability matrix written).
- [ ] Iteration Gate checklist evaluated against effective gates for the maturity level.
- [ ] Increment report written.
- [ ] Gate **pass** → system merged to `main`; increment branches and worktrees pruned.
- [ ] Gate **fail** → `passed: false` reported with full details.

---

## Persist

```
Trace path        : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/10-acceptance-test.md
Traceability      : docs/hephaestus/traceability/<INC_ID>.md
Assumption ledger : docs/hephaestus/debt/assumptions.md
Content           : heading "Acceptance Test — <INC_ID> @ <level>"
                    bullets: scenarios run, evidence, promotions, gate result, status
Commit msg        : hephaestus(<level>/<INC_ID>): Iteration-Gate
```
