<!-- EXAMPLE (filled) · Agent bundle · Agent-owned -->

# PLAN — developer-01

> The developer's route for milestones 2 and 4 of `../../../Human/ROADMAP.md`.

## Anchors

- **Goal & fence:** `../../../Human/BRIEF.md`
- **Time plan (PM-owned):** `../../../Human/ROADMAP.md`
- **Identity & limits:** `./AGENT.md`

## Mission (one sentence)

> Turn the approved contract into a working FastAPI service that creates links,
> redirects fast, and counts clicks — then deploy it to staging.

## Milestones this agent owns

| Milestone | Approach | Done when |
| --- | --- | --- |
| 2 · Core service | Scaffold → create → redirect → stats, each with tests | Endpoints work end-to-end locally |
| 4 · Staging deploy | Containerize → migrate → deploy → runbook | Live on staging + runbook written |

## Working approach

- **Decomposition:** milestones → tasks in `./TASKS.md`, matured draft → reviewed → accepted.
- **Loop:** pick task → write test → implement → self-check vs contract → update ./TASKS.md (PM rolls up).
- **Latency note:** cache code→URL lookups in-process; redirect handler avoids ORM overhead.
- **Definition of done:** all three endpoints match `openapi.yaml`; migrations run
  clean; smoke test green before hand-off to tester.

## Risks & blockers

| Risk / blocker | Impact | Mitigation | Needs a human? |
| --- | --- | --- | --- |
| p95 latency target hard to hit via ORM | M | Raw query on hot redirect path | no |
| Staging deploy is past autonomy boundary | H | Request approval before deploying | yes → DECISIONS.md |
