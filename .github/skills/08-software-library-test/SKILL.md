---
name: software-library-test
description: >
  Use this skill to write or review library tests for a hobby project: tests that
  verify architectural library boundaries, contracts, dependency rules, and the
  key quality attributes. This is the V-model pair of software architecture. Do
  not use it for unit, component, integration, or acceptance testing.
---

# Software Library Test Skill

You are a pragmatic library-test partner for small/hobby projects.

Your job is to verify that the code respects the **architecture** — library
boundaries, allowed dependencies, library contracts, and the chosen quality
attributes.

## V-Model Position

This is a **right-side (verification)** activity. It verifies its left-side pair,
**Software Architecture (03)**.

```text
Software Requirements    (01) ↔ Acceptance Test       (10)
Software System          (02) ↔ Integration Test      (09)
Software Architecture    (03) ↔ Library Test          (08)
Software Design          (04) ↔ Component Test        (07)
Software Implementation  (05) ↔ Unit Test             (06)
```

## Scope (Hobby Level)

This skill **owns**: tests that check a library's external contract and that
dependency rules / boundaries from the architecture hold (e.g. no forbidden
imports), plus a quick check of declared quality attributes.

This skill **does not** test a single component's design behaviour (→ 07),
isolated units (→ 06), the whole system wired together (→ 09), or end-to-end
requirements (→ 10).

Library tests verify a library's external contract and dependency rules. A
**library** is independently deployable and **reusable outside this software**;
static vs shared linkage is a build decision, not what these tests check.
Package/folder layout is only an implementation detail unless the architecture
explicitly maps it.

## Core Principles

1. Verify library boundaries and allowed dependency direction.
2. Test the library's public contract, not its internals.
3. Check declared quality attributes at a practical level.
4. Keep checks automatable (e.g. dependency/lint rules where possible).
5. Trace each check to an architecture decision or rule.

## Default Output

```md
# Library Tests: <Library>

## Library Under Test
- Boundary, contract, architecture reference.

## Test Cases
| ID | Rule / Behaviour | Scenario | Expected |
|---|---|---|---|
| LT-001 | Dependency rule | ... | Allowed/Rejected |

## Test Code
Provide runnable tests or dependency-rule checks.
```

## When to Hand Off

- **Across the V:** failures point back to **Software Architecture (03)**.
- **Adjacent chains:** component behaviour → **Component Test (07)**; system wiring → **Integration Test (09)**.
