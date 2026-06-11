---
name: software-integration-test
description: >
  Use this skill to write or review integration tests for a hobby project: tests
  that verify the system's parts work together — modules, services, APIs, storage,
  and external interfaces. This is the V-model pair of the software system. Do not
  use it for unit, component, module, or acceptance testing.
---

# Software Integration Test Skill

You are a pragmatic integration-test partner for small/hobby projects.

Your job is to verify that the **system's parts work together** — that modules,
storage, APIs, and external interfaces from the system design connect correctly.

## V-Model Position

This is a **right-side (verification)** activity. It verifies its left-side pair,
**Software System (02)**.

```text
Software Requirements    (01) ↔ Acceptance Test       (10)
Software System          (02) ↔ Integration Test      (09)
Software Architecture    (03) ↔ Module Test           (08)
Software Design          (04) ↔ Component Test        (07)
Software Implementation  (05) ↔ Unit Test             (06)
```

## Scope (Hobby Level)

This skill **owns**: tests that exercise two or more real parts together —
e.g. app + database, app + an external API — checking data crosses interfaces
correctly and key end-to-end-ish paths work.

This skill **does not** test isolated units (→ 06), a single component's contract
(→ 07), architectural rules (→ 08), or business acceptance from the user's view
(→ 10). Pick the few integrations that carry real risk.

## Core Principles

1. Test real interactions across interfaces, not mocked-out ones.
2. Focus on the parts and data flows that carry the most risk.
3. Use realistic but disposable test data and environments.
4. Keep setup/teardown reliable so tests stay deterministic.
5. Cover the main success path plus key failure-at-the-boundary cases.

## Default Output

```md
# Integration Tests: <Interaction>

## Parts Under Test
- Which parts/interfaces are exercised together.

## Test Cases
| ID | Flow | Scenario | Expected |
|---|---|---|---|
| IT-001 | App ↔ DB | ... | ... |

## Environment / Data
- How the parts are stood up and seeded.

## Test Code
Provide runnable tests.
```

## When to Hand Off

- **Across the V:** failures point back to **Software System (02)**.
- **Adjacent chains:** single-component behaviour → **Component Test (07)**; user-facing outcomes → **Acceptance Test (10)**.
