<!-- EXAMPLE (filled) · Agent bundle · Agent-owned -->

# PLAN — tester

> The tester's route for milestone 3 of `../../../Human/ROADMAP.md`.

## Anchors

- **Goal & fence:** `../../../Human/BRIEF.md`
- **Time plan (PM-owned):** `../../../Human/ROADMAP.md`
- **Identity & limits:** `./AGENT.md`

## Mission (one sentence)

> Prove — with automated evidence — that Snip meets every success criterion before
> it goes to staging.

## Milestones this agent owns

| Milestone | Approach | Done when |
| --- | --- | --- |
| 3 · Test & harden | Contract tests → unit/integration → load → security | ≥90% coverage, p95<50ms, no open redirect |

## Criterion → check mapping

| Brief success criterion | Test that proves it |
| --- | --- |
| create + 302 redirect | integration test: create then follow redirect |
| stats endpoint | integration test: N clicks → count == N |
| p95 < 50 ms @ 100 rps | load test in CI (fails build if exceeded) |
| ≥90% core coverage | coverage gate in CI |
| no open redirect | security test: reject external/looping/non-allow-list targets |

## Working approach

- **Loop:** write check for a criterion → run vs dev branch → record result in
  STATUS → file blocking defects back to developer.
- **Definition of done:** every criterion above is green in CI; report attached.

## Risks & blockers

| Risk / blocker | Impact | Mitigation | Needs a human? |
| --- | --- | --- | --- |
| Borderline p95 result | M | Re-run, profile, escalate if still borderline | maybe → DECISIONS.md |
| Allow-list undefined blocks security test | H | Depends on D-1 resolution | yes → DECISIONS.md |
