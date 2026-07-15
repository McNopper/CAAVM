<!-- EXAMPLE (filled) · Agent bundle · Agent-owned -->

# PLAN — architect

> The architect's route for milestone 1 of `../../../Human/ROADMAP.md`.

## Anchors

- **Goal & fence:** `../../../Human/BRIEF.md`
- **Time plan (PM-owned):** `../../../Human/ROADMAP.md`
- **Identity & limits:** `./AGENT.md`

## Mission (one sentence)

> Produce an approved, contract-first API design that makes fast redirects and
> open-redirect safety structural, so the developer can build without guesswork.

## Milestones this agent owns

| Milestone | Approach | Done when |
| --- | --- | --- |
| 1 · Approved design | Draft OpenAPI contract → schema → ADRs → review | Design reviewed; ADRs recorded |

## Working approach

- **Decomposition:** milestone → tasks in `./TASKS.md`.
- **Loop:** draft contract → self-check vs success criteria → update ./TASKS.md (PM rolls up) →
  raise open questions in DECISIONS.
- **Key decisions to settle:** short-code scheme (base62, 7 chars), redirect status
  (302 vs 301 — pick 302 to keep stats), allow-list vs deny-list for targets.
- **Definition of done:** `openapi.yaml` covers all three endpoints; schema supports
  click counting; ADR documents the open-redirect defense.

## Risks & blockers

| Risk / blocker | Impact | Mitigation | Needs a human? |
| --- | --- | --- | --- |
| 301 caching would break click stats | M | Choose 302; note in ADR | no |
| Open-redirect policy may reject legit marketing hosts | M | Propose allow-list of campaign domains | yes → DECISIONS.md |
