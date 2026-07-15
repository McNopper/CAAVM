<!-- EXAMPLE (filled) · Planning · BOARD · Owned by the PM agent · Human-facing -->

# BOARD — the live planning board

> Snip's aggregated Kanban, maintained by the PM from each instance's `TASKS.md`.

## Project

- **Project:** Snip — a URL-shortener REST API
- **Maintained by:** `../AI/agents/pm/`
- **Last updated:** 2026-07-14 12:10

## Summary (for the human)

| Column | Count | Notes |
| --- | --- | --- |
| Backlog | 3 | later milestones |
| Todo | 3 | ready to start |
| In progress | 4 | one per active instance |
| Blocked | 3 | ⚠️ all on D-1 — see `./DECISIONS.md` |
| Review | 0 | — |
| Done | 5 | design groundwork + setup |

---

## Kanban

```mermaid
kanban
  backlog[Backlog]
    b1[Load test scenario @ 100 rps]@{ assigned: 'tester-01', priority: 'High' }
    b2[Staging deploy + runbook]@{ assigned: 'developer-01', priority: 'Low' }
    b3[Coverage gate to 90%]@{ assigned: 'tester-01', priority: 'Low' }
  todo[Todo]
    t1[Finalize openapi.yaml]@{ assigned: 'architect-01', priority: 'High' }
    t2[Implement POST /links]@{ assigned: 'developer-01', priority: 'High' }
    t3[Contract tests from openapi.yaml]@{ assigned: 'tester-01', priority: 'High' }
  wip[In progress]
    w1[Draft ADR-002 open-redirect defense]@{ assigned: 'architect-01', priority: 'Very High' }
    w2[Scaffold app + write path]@{ assigned: 'developer-01', priority: 'High' }
    w3[Redirect hot-path design]@{ assigned: 'developer-02', priority: 'High' }
    w4[Write executable test plan]@{ assigned: 'tester-01', priority: 'High' }
  blocked[Blocked]
    x1[Finalize ADR-002]@{ assigned: 'architect-01', priority: 'Very High', ticket: 'D-1' }
    x2[Security tests: open-redirect]@{ assigned: 'tester-01', priority: 'Very High', ticket: 'D-1' }
    x3[Implement GET /code redirect]@{ assigned: 'developer-02', priority: 'High', ticket: 'D-1' }
  review[Review]
  done[Done]
    d1[ADR-001 short-code scheme]@{ assigned: 'architect-01', priority: 'High' }
    d2[Endpoint list + status codes]@{ assigned: 'architect-01', priority: 'High' }
    d3[Repo + CI skeleton]@{ assigned: 'developer-01', priority: 'High' }
    d4[Agree shared schema]@{ assigned: 'developer-02', priority: 'High' }
    d5[Chose load tool + coverage gate]@{ assigned: 'tester-01', priority: 'Low' }
```

---

## Blocked cards → decisions

| Card | Owner instance | Blocked on | Decision id |
| --- | --- | --- | --- |
| Finalize ADR-002 | architect-01 | which redirect targets are allowed | D-1 |
| Security tests: open-redirect | tester-01 | allow-list definition | D-1 |
| Implement GET /{code} redirect | developer-02 | contract approval (waits on D-1) | D-1 |
