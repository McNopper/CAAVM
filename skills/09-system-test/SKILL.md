---
name: 09-system-test
description: Hephaestus V-Model Stage 9. Verify all deployables of a system run together in the topology. Use after module tests pass for all modules in a deployable. Reports results only; no version control.
---

# Skill 09 — System Test Verifier · V-Model Stage 9

> Paired with **Stage 2 — Software System** (you verify the topology and deployables that
> the Systems Architect defined).
>
> **Role:** verify that each deployable's modules compose correctly into the executable
> and that it participates correctly in the system topology with the other deployables.
> You are a *different agent than the implementer* — adversarial.

---

## When to use

After Stage 08 reports `passed: true` for all modules belonging to a deployable.
Run per deployable, in parallel across deployables.

---

## Built-in defaults

```yaml
language: C++23
build: CMake ≥ 3.28 + Ninja
test_system: ctest
quality_gates:
  lint_warnings_max: 0
agile:
  max_fix_rounds: 2
```

> **To retarget:** prepend your own config before invoking.

---

## Inputs

| Input | Description |
|-------|-------------|
| `deployable` | Deployable name, kind, and interface (from Stage 02) |
| `assembled_deployable` | The already-assembled deployable with its module composition and wiring |
| `system_test_plan` | From Stages 02/03 — how deployables run together in the topology |
| `software_system` | Stage 02 output — topology, all deployable names and interfaces |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `inc_id` | Increment ID |

---

## Process

### Step 1 — Verify the assembled deployable

The agent receives the assembled deployable and verifies that its module composition,
entry-point wiring, and topology participation behave as expected.

Write or adjust the executable-level CMake glue as needed:
- Link all modules into the deployable (static libs linked in, shared libs / DLLs
  resolved at load time, per the architecture's packaging choices).
- Wire the entry point.

```
cmake --build build --target <deployable_target>
```

### Step 2 — Run the system test for this deployable

```
ctest -R <deployable_name>_system
```

Build **only** this deployable's target. External systems may be mocked; the real
executable itself is under test.

### Step 3 — Verify adversarially

Using the `system_test_plan`:
- Run every item in the plan for this deployable.
- Test the deployable's interface under realistic conditions: network errors,
  unexpected input, concurrent connections (if applicable).
- Verify the deployable interacts correctly with the other deployables.
- Intentionally-deferred NFRs are out of scope at `mvp`/`harden`.

### Step 4 — Targeted repair (if red)

1. Identify the failure: module assembly, entry-point wiring, or inter-deployable
   communication.
2. Fix **only** the failing deployable's integration within the assembled deployable under test.
3. Re-run the system test.
4. Repeat up to **`max_fix_rounds: 2`** times.
5. If still red: report `passed: false`; the climb stops.

**Do not touch other deployables.**

### Step 5 — Report results

Report results only — perform no git or version-control operations.

---

## Output

```
level  : system
scope  : <deployable name>
passed : true | false
details: <system test results, topology participation, fix rounds used>
```

---

## Exit criteria

- [ ] Deployable executable builds in isolation.
- [ ] All system test plan items for this deployable verified.
- [ ] Deployable interacts correctly in the topology with the other deployables.
- [ ] `passed: true` OR `passed: false` with full details after fix budget.

---

## Persist

```
Trace path : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/09-system-test-<deployable>.md
Content    : heading "System Test — <deployable> @ <level>"
             bullets: assembled deployable verified, test results, topology participation, fix rounds, status
```
