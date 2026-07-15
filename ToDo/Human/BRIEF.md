<!--
  TEMPLATE · Mandate · Human-authored · Agents READ-ONLY
  Owner:     Humans (the customer + whoever holds the project)
  Written:   Up front, before any agent runs. Changed only by humans.
  Essence:   The immutable direction — who it's for, what success is, where the fence is.
  Agents:    May read every line; may NOT edit this file.
  Location:  This file lives in Human/ — the only folder a human needs to open.
  How used:  The human hands this to the PM agent (../AI/agents/pm/). The PM turns it
             into ./ROADMAP.md (Gantt) and ./BOARD.md (Kanban), decides which worker
             agents to spawn in AI/, and reports back via ./STATUS.md.
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# BRIEF — the mandate

> The **direction** the human sets. This single file fixes *who the work is for*,
> *what "done and successful" means*, and *what is off-limits*. The human gives it to
> the **PM agent**, who plans and runs the work from it. It is the yardstick every
> agent is measured against and what the human observer watches progress against.
> Agents treat it as read-only truth.

## Project

- **Project:** {{ project name }}
- **Human owner (accountable):** {{ who signs off }}
- **Last updated (by a human):** {{ date }}

---

## 1 · Customers & stakeholders (the why)

Real people the work serves. They are *served*, never *acting* — agents never
speak for them; only the human owner does.

| Customer / stakeholder | Need or interest | Authority | Constraints they impose |
| --- | --- | --- | --- |
| {{ e.g. paying customer }} | {{ what they want }} | {{ sign-off / veto / none }} | {{ deadline, budget, policy }} |
| {{ e.g. end user }} | {{ jobs to be done }} | none (served) | {{ accessibility, locale }} |

- **Final say / escalation target:** {{ the human who decides }}

---

## 2 · Goal (the destination)

> {{ In one sentence: what does "done and successful" look like? }}

### Success criteria (how agents and the observer know it's met)

- [ ] {{ measurable outcome 1 }}
- [ ] {{ measurable outcome 2 }}
- [ ] {{ measurable outcome 3 }}

### Metrics

| Metric | Baseline | Target | How measured |
| --- | --- | --- | --- |
| {{ metric }} | {{ now }} | {{ target }} | {{ source }} |

### Non-negotiables

Hard limits any execution must respect (deadline, budget, legal, tech).

- {{ constraint }}

---

## 3 · Scope (the fence)

**In scope — agents will build:**

- {{ item }}

**Out of scope — agents will not build:**

- {{ item }}

**Explicit non-goals — anti-targets that stop agents gold-plating or overlapping:**

- {{ non-goal }}

### Autonomy boundary

What agents may decide alone vs. what must stop for a human. This is the core of
"human as observer": agents run freely inside the line and pause at it.

| Agents may decide alone | Agents must pause for a human |
| --- | --- |
| {{ e.g. implementation details }} | {{ e.g. scope changes, irreversible actions, spending }} |

> Anything past this line **bubbles up to the PM** (in `../AI/agents/pm/INBOX.md`).
> The PM escalates to this side — `./DECISIONS.md` — only what truly needs a human.
