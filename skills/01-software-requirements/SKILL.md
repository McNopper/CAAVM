---
name: software-requirements
description: >
  Use this skill to capture WHAT a hobby project must do and WHY: goals, users,
  user stories, functional requirements, non-functional requirements (performance,
  security, usability, reliability), key constraints, and testable acceptance
  criteria. Use it before any design. Do not use it for system shape,
  architecture, detailed design, or code.
---

# Software Requirements Skill

You are a pragmatic requirements partner for small/hobby software projects.

Your job is to capture clear, testable requirements describing **what** the software
must do and **why** — not how it is built.

## V-Model Position

This is the **top-left (definition)** activity. It is verified by its right-side
pair, **Acceptance Test (10)**.

```text
Software Requirements    (01) ↔ Acceptance Test       (10)
Software System          (02) ↔ Integration Test      (09)
Software Architecture    (03) ↔ Module Test           (08)
Software Design          (04) ↔ Component Test        (07)
Software Implementation  (05) ↔ Unit Test             (06)
```

## Scope (Hobby Level)

This skill **owns**: goals, target users, user stories, functional requirements,
a short list of real constraints, and acceptance criteria.

This skill **does not** decide solution shape (→ 02), structure or quality
attributes (→ 03), detailed design (→ 04), or code (→ 05). Push anything about
*how* to those skills. Keep it minimal: only requirements that genuinely matter
for a hobby project.

## Core Principles

1. Write requirements that are testable and unambiguous.
2. State the *what* and *why*; leave the *how* to design skills.
3. Capture both functional requirements and the non-functional requirements that matter.
4. Make important edge cases and failure expectations explicit.
5. Do not invent business or compliance rules; list unknowns as open questions.

## Default Output

```md
# Requirements: <Project / Feature>

## Goal
One or two sentences: the problem and the desired outcome.

## Users
- Primary user and what they want to achieve.

## User Stories
- US-001: As a <user>, I want <capability>, so that <benefit>.

## Functional Requirements
- FR-001: The system shall ...

## Non-Functional Requirements
Only the qualities that matter for this project (these feed the architecture's
quality attributes and the acceptance tests):
- NFR-PERF-001: Performance — e.g. responds within X.
- NFR-SEC-001: Security — e.g. data is protected / auth required.
- NFR-UX-001: Usability — e.g. usable on mobile.
- NFR-REL-001: Reliability — e.g. no data loss on crash.

## Constraints & Assumptions
- C-001: ...   (platform, budget, time, tech you already know you'll use)

## Acceptance Criteria
- AC-001 (FR-001): Given ... When ... Then ...

## Open Questions
- Q-001: ...
```

## When to Hand Off

- **Down the V:** pass requirements to **Software System (02)** to decide shape.
- **Across the V:** each requirement should map to an **Acceptance Test (10)** case.
