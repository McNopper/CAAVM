<!-- EXAMPLE (filled) · Capability pool · Pooled -->

# SKILL — contract-first-api

> Worked example of a reusable, goal-neutral method. Loaded by the architect
> (to design), the developer (to implement to spec), and the tester (to generate
> contract tests). It describes *how*, never *why*.

## Identity

- **Skill:** contract-first-api
- **Purpose:** Design and build an HTTP API from an OpenAPI contract as the single
  source of truth, so design, code, and tests never drift.
- **Owner / maintainer:** platform guild
- **Last updated:** 2026-07-14

## When to use

- **Use when:** building or changing an HTTP/REST API.
- **Do not use when:** the interface is an internal function call or event stream.

## Inputs → Outputs

| Inputs required | Produces |
| --- | --- |
| Endpoint list + data model | Validated `openapi.yaml`, generated stubs, contract tests |

## Procedure

1. Draft `openapi.yaml` (paths, schemas, status codes) and validate it.
2. Review the contract with stakeholders before writing code.
3. Generate server stubs and client/contract tests from the contract.
4. Implement to the stubs; treat any drift from the contract as a defect.
5. Run contract tests in CI so the contract stays authoritative.

## Guardrails

- The contract is the source of truth — code changes never silently alter it.
- Respect the active project's `../../Human/BRIEF.md` scope & non-goals.

## Used by

- architect (authors it), developer (implements to it), tester (tests against it).
