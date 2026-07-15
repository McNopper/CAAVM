<!-- EXAMPLE (filled) · Observe · STATUS · Curated by the PM agent · Human-facing -->

# STATUS — the observer's dashboard

> Written by Snip's PM for Dana. This is the one screen she watches.

## Headline

> On track for the Aug 21 staging ship, but one decision (**D-1**, the open-redirect
> policy) is blocking design and the security tests and needs your call today.

| | |
| --- | --- |
| **Overall health** | 🟡 at risk |
| **Current milestone** | m1 · Approved API design |
| **Target ship** | 2026-08-21 · confidence 🟡 medium |
| **Awaiting you** | ⚠️ D-1 in `./DECISIONS.md` |
| **Updated** | 2026-07-14 10:52 by PM |

## Progress vs the goal

| Success criterion | Progress | State |
| --- | --- | --- |
| create + 302 redirect | `▓▓▓▓▓░░░░░` 50% | 🔵 in progress |
| stats endpoint | `▓▓░░░░░░░░` 20% | 🔵 in progress |
| p95 < 50 ms @ 100 rps | `░░░░░░░░░░` 0% | ⚪ not started |
| ≥90% core coverage | `░░░░░░░░░░` 0% | ⚪ not started |
| no open redirect | `▓░░░░░░░░░` 10% | 🔴 blocked (D-1) |

## The team right now

| Instance | Doing now | Health | Waiting on |
| --- | --- | --- | --- |
| pm | Escalating D-1 to Dana | 🟢 | Dana's decision |
| architect-01 | Drafting ADR-002 (open-redirect) | 🟡 | D-1 |
| developer-01 | Building write path (`POST /links`) | 🟢 | — |
| developer-02 | Redirect hot-path design | 🟡 | D-1 (contract) |
| tester-01 | Writing executable test plan | 🟢 | first build |

## Needs a human (from DECISIONS.md)

| Decision | What we need | Since | Blocking? |
| --- | --- | --- | --- |
| D-1 | Pick allow-list / deny-list / hybrid for redirect targets (PM recommends hybrid) | 10:22 | ⚠️ yes — 3 cards parked |

## Recently shipped (newest first)

- 12:10 — developer-02 — shared schema agreed, read-path design ready
- 11:55 — developer-01 — app scaffold + migrations
- 10:25 — architect-01 — ADR-001 + endpoint list; D-1 raised
- 10:10 — pm — roadmap (Gantt) + board (Kanban) published

## Pointers

- Time plan → `./ROADMAP.md` (Gantt)
- Live work → `./BOARD.md` (Kanban)
- Open decisions → `./DECISIONS.md`
