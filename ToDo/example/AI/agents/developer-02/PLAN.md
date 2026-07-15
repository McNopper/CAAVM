<!-- EXAMPLE (filled) · Worker instance · Lives in AI/ -->

# PLAN — developer-02

> developer-02's route: the read path of Snip.

## Anchors (read-only)

- **Goal & fence:** `../../../Human/BRIEF.md`
- **Time plan (PM-owned):** `../../../Human/ROADMAP.md`
- **Identity & limits:** `./AGENT.md`

## Mission (one sentence)

> Build a fast `GET /{code}` redirect that records a click, plus the stats endpoint,
> hitting the p95 < 50 ms target.

## Milestones / lane this instance owns

| Lane item | Approach | Done when |
| --- | --- | --- |
| Redirect + click record | Raw query on hot path; in-process code→URL cache | 302 works; count increments |
| Stats endpoint | Aggregate clicks by code | returns total clicks |

## Working approach

- **Session:** open `./runs/<date>T<HH-MM-SS>/`, work the loop, log RUN.md.
- **Loop:** pick top task → execute → self-check vs the brief → update `./TASKS.md`
  → bubble up any blocker in `../pm/INBOX.md`.
- **Coordination:** shares the app skeleton + schema with developer-01; no edits to
  the write path.

## Risks & blockers

| Risk / blocker | Impact | Mitigation | Bubble up? |
| --- | --- | --- | --- |
| ORM overhead on redirect path | M | Raw SQL on hot path | no |
| Schema churn from developer-01 | M | Agree schema before both start | yes → ../pm/INBOX.md if it changes |
