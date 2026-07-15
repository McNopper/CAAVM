<!--
  OBSERVE · STATUS · Curated by the PM agent · Human-facing (ENRICHED)
  Owner:     ../AI/agents/pm/  (the PM translates raw agent progress into human language)
  Reader:    The human observer
  Essence:   The one screen the human watches. A dashboard, not a log — health,
             progress vs goal, what each agent is doing, and what needs the human.
  Note:      This is the richest human interface artifact. Keep it plain-language,
             visual, and current. Raw detail stays in agents/<role>/TASKS.md.
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# STATUS — the observer's dashboard

> Written **by the PM, for the human**. If the human reads only one file, it is this
> one. It translates the team's raw work into a legible picture: are we on track,
> what just happened, and is anything waiting on you?

## Headline

> {{ One sentence a human can act on. e.g. "On track for Fri ship; one decision
> (D-1) is blocking the security work and needs your call today." }}

| | |
| --- | --- |
| **Overall health** | {{ 🟢 on track / 🟡 at risk / 🔴 blocked }} |
| **Current milestone** | {{ # and name (from ROADMAP.md) }} |
| **Target ship** | {{ date }} · confidence {{ 🟢/🟡/🔴 }} |
| **Awaiting you** | {{ nothing / DECISIONS.md D-x }} |
| **Updated** | {{ timestamp }} by PM |

## Progress vs the goal

The scorecard — every `./BRIEF.md` success criterion, in plain language.

| Success criterion | Progress | State |
| --- | --- | --- |
| {{ criterion }} | `▓▓▓▓▓░░░░░` {{ 50% }} | {{ 🔵 in progress }} |
| {{ criterion }} | `░░░░░░░░░░` {{ 0% }} | {{ ⚪ not started }} |
| {{ criterion }} | `▓▓▓▓▓▓▓▓▓▓` {{ 100% }} | {{ ✅ met }} |

## The team right now

Snapshot of each agent (the PM rolls this up from their `TASKS.md`).

| Agent | Doing now | Health | Waiting on |
| --- | --- | --- | --- |
| project manager | {{ coordinating … }} | {{ 🟢 }} | — |
| {{ architect }} | {{ current task }} | {{ 🟡 }} | {{ D-1 }} |
| {{ developer }} | {{ current task }} | {{ 🟢 }} | — |
| {{ tester }} | {{ current task }} | {{ 🟢 }} | {{ upstream build }} |

## Needs a human (from DECISIONS.md)

| Decision | What we need | Since | Blocking? |
| --- | --- | --- | --- |
| {{ D-1 }} | {{ the call required }} | {{ time }} | {{ ⚠️ yes }} |

## Recently shipped (newest first)

- {{ time }} — {{ agent }} — {{ what completed }}

## Pointers

- Time plan → `./ROADMAP.md` (Gantt)
- Live work → `./BOARD.md` (Kanban)
- Open decisions → `./DECISIONS.md`
