<!--
  PLANNING · ROADMAP · Owned by the PM agent · Everyone reads
  Owner:     ../AI/agents/pm/  (generated from ./BRIEF.md; humans read, don't edit)
  Essence:   The TIME plan — which agents, which milestones, in what order, by when.
             Visualized as a Mermaid GANTT chart so the human sees the schedule.
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# ROADMAP — the time plan

> The PM's schedule for reaching the `./BRIEF.md` goal. This is a
> **human-facing** artifact — enriched with a Gantt, a plain-language summary, and a
> legend, so a human observer understands it at a glance. The **Gantt** is the
> canonical view; the tables under it carry the detail agents execute against. When
> reality shifts, the PM updates this — worker agents read it, they do not edit it.

## Project

- **Project:** {{ project name }}
- **Maintained by:** `../AI/agents/pm/`
- **Derived from:** `./BRIEF.md`
- **Last updated:** {{ date }}

## At a glance (for the human)

> {{ 2–3 plain sentences: where the project is, what's next, any risk. Written by
> the PM for a human skimming in 10 seconds. }}

- **Target ship date:** {{ date }}  ·  **Confidence:** {{ 🟢 high / 🟡 medium / 🔴 low }}
- **Now:** {{ current milestone }}  ·  **Next:** {{ next milestone }}
- **Top risk:** {{ one line, or "none" }}

**Legend:** `done` ✅ complete · `active` 🔵 in progress · `crit` 🔴 critical path ·
`milestone` ◆ key date.

---

## Gantt — milestones over time

> Task states you can use: `done`, `active`, `crit` (critical), `milestone`.
> Sequence with `after <id>`; give durations like `5d`, `2w`.

```mermaid
gantt
  title {{ project name }} — Roadmap
  dateFormat YYYY-MM-DD
  axisFormat %b %d

  section Design
    {{ milestone 1 }}        :done,   m1, {{ 2026-07-14 }}, {{ 5d }}
  section Build
    {{ milestone 2 }}        :active, m2, after m1, {{ 10d }}
    {{ milestone 3 }}        :crit,   m3, after m2, {{ 6d }}
  section Verify & Ship
    {{ milestone 4 }}        :        m4, after m3, {{ 4d }}
    Ship to {{ target }}     :milestone, ship, after m4, 0d
```

---

## Agents on this project

Which roles the PM chose to spawn (one row → one `../AI/agents/<role>/` bundle).

| Agent | Function | Owns milestones | Serves brief need |
| --- | --- | --- | --- |
| project manager | Coordinates, plans, reports (always present) | all (oversight) | keep humans in control |
| {{ architect }} | {{ shapes the solution }} | {{ m1 }} | {{ need }} |
| {{ developer }} | {{ builds it }} | {{ m2, m4 }} | {{ need }} |
| {{ tester }} | {{ verifies vs the goal }} | {{ m3 }} | {{ need }} |

## Milestone detail

| # | Milestone | Owner agent | Done when (brief success criterion) |
| --- | --- | --- | --- |
| m1 | {{ milestone }} | {{ agent }} | {{ criterion }} |
| m2 | {{ milestone }} | {{ agent }} | {{ criterion }} |
| m3 | {{ milestone }} | {{ agent }} | {{ criterion }} |
| m4 | {{ milestone }} | {{ agent }} | {{ criterion }} |

## Hand-offs

| From | Artifact | To | Accepted when |
| --- | --- | --- | --- |
| {{ agent }} | {{ artifact }} | {{ agent }} | {{ condition }} |

## Observer check-ins

Where the human deliberately reviews or must approve (past the autonomy boundary).

| Checkpoint | Trigger | Human reviews |
| --- | --- | --- |
| {{ after m1 }} | {{ milestone done }} | `./STATUS.md` + deliverable |
| {{ before ship }} | {{ m4 done }} | readiness; deploy needs approval |
