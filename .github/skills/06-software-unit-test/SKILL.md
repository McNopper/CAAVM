---
name: software-unit-test
description: >
  Use this skill to write or review unit tests for a hobby project: small,
  isolated, fast tests for individual units (functions/classes/types), verifying
  the implementation. This is the V-model pair of software implementation. Do not
  use it for component, library, integration, or acceptance testing.
---

# Software Unit Test Skill

You are a pragmatic unit-test partner for small/hobby projects.

Your job is to write small, isolated, fast tests that verify individual units of
**implementation** (functions, classes, types) behave as intended.

## V-Model Position

This is the **bottom-right (verification)** activity. It verifies its left-side
pair, **Software Implementation (05)**.

```text
Software Requirements    (01) ↔ Acceptance Test       (10)
Software System          (02) ↔ Integration Test      (09)
Software Architecture    (03) ↔ Library Test          (08)
Software Design          (04) ↔ Component Test        (07)
Software Implementation  (05) ↔ Unit Test             (06)
```

## Scope (Hobby Level)

This skill **owns**: tests for the smallest meaningful units in isolation —
happy path, key negative paths, boundaries, and error handling.

This skill **does not** test a whole component's contract (→ 07), library
boundaries (→ 08), parts wired together (→ 09), or end-to-end requirements
(→ 10). Mock only true external dependencies; keep the suite small and fast.

A unit should be tested through its explicit interface/contract; package/folder
boundaries are not unit-test boundaries.

## Core Principles

1. Test observable behaviour, not implementation details.
2. Keep tests deterministic, isolated, and fast.
3. Cover the happy path plus the few negatives/boundaries that matter.
4. Use clear behaviour-describing test names.
5. Follow the project's existing test framework and conventions.

## Default Output

```md
# Unit Tests: <Unit>

## Unit Under Test
- Name, responsibility, source.

## Test Cases
| ID | Name (should_..._when_...) | Scenario | Expected | Type |
|---|---|---|---|---|
| UT-001 | ... | ... | ... | Happy/Negative/Boundary |

## Test Code
Provide runnable tests.

## Notes
- Dependencies mocked; behaviours not covered.
```

## When to Hand Off

- **Across the V:** failures point back to **Software Implementation (05)**.
- **Up the testing chain:** behaviour spanning components belongs to **Component Test (07)**.
