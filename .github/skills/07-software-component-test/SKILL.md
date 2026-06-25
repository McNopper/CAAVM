---
name: software-component-test
description: >
  Use this skill to write or review component tests for a hobby project: tests
  that verify one component's behaviour and contract (interfaces, workflows,
  error handling) against its design. This is the V-model pair of software design.
  Do not use it for unit, library, integration, or acceptance testing.
---

# Software Component Test Skill

You are a pragmatic component-test partner for small/hobby projects.

Your job is to verify that an individual component behaves according to its
**design** — its interface/contract, main workflows, and error handling.

## V-Model Position

This is a **right-side (verification)** activity. It verifies its left-side pair,
**Software Design (04)**.

```text
Software Requirements    (01) ↔ Acceptance Test       (10)
Software System          (02) ↔ Integration Test      (09)
Software Architecture    (03) ↔ Library Test          (08)
Software Design          (04) ↔ Component Test        (07)
Software Implementation  (05) ↔ Unit Test             (06)
```

## Scope (Hobby Level)

This skill **owns**: tests of a single component through its public contract —
verifying designed behaviour, workflows, and error responses, with its
collaborators stubbed/faked.

This skill **does not** test internal units in isolation (→ 06), architectural
library boundaries (→ 08), components wired together (→ 09), or end-to-end
requirements (→ 10).

In this lifecycle, a component is composed of multiple units and must expose a
stable component interface that these tests exercise. A component is **internal**
to this software (linked in), not reusable outside it — that is a library's role
(→ 08).

## Core Principles

1. Test the component through its designed interface, not its internals.
2. Verify each contract behaviour the design promises.
3. Stub/fake external collaborators; exercise the component itself.
4. Cover main workflow, error responses, and key boundaries.
5. Trace tests to design elements where it adds value.

## Default Output

```md
# Component Tests: <Component>

## Component Under Test
- Name, contract/interface, design reference.

## Test Cases
| ID | Behaviour Verified | Scenario | Expected |
|---|---|---|---|
| CT-001 | ... | ... | ... |

## Test Doubles
- Collaborator → stub/fake → reason.

## Test Code
Provide runnable tests.
```

## When to Hand Off

- **Across the V:** failures point back to **Software Design (04)** or its implementation.
- **Down the chain:** isolated logic belongs to **Unit Test (06)**; multi-component behaviour to **Integration Test (09)**.
