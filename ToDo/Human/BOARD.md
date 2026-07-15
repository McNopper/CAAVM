<!--
  PLANNING · BOARD · Owned by the PM agent · Human-facing (ENRICHED)
  Owner:     ../AI/agents/pm/  (aggregates every agent's TASKS.md; humans read, don't edit)
  Essence:   The live project planning board — what is flowing right now, across all
             agents. Visualized as a Mermaid KANBAN so a human sees WIP at a glance.
  Note:      This is a HUMAN interface artifact — keep it legible, labelled, and
             summarized. The agents' own raw work lives in each agents/<role>/TASKS.md.
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# BOARD — the live planning board

> The PM's aggregated **Kanban**. Every card is a real work item pulled from an
> agent's `TASKS.md`, tagged with who owns it, so a human observer can see the whole
> team's flow in one picture. The PM keeps this current; agents do not edit it.

## Project

- **Project:** {{ project name }}
- **Maintained by:** `../AI/agents/pm/`
- **Last updated:** {{ date }}

## Summary (for the human)

| Column | Count | Notes |
| --- | --- | --- |
| Backlog | {{ n }} | {{ note }} |
| Todo | {{ n }} | {{ note }} |
| In progress | {{ n }} | WIP limit {{ k }} |
| Blocked | {{ n }} | ⚠️ see `./DECISIONS.md` |
| Review | {{ n }} | {{ note }} |
| Done | {{ n }} | {{ note }} |

> **Priority** values: `Very High`, `High`, `Low`, `Very Low`.
> **assigned** = the agent that owns the card. **ticket** = optional external id.

---

## Kanban

```mermaid
kanban
  backlog[Backlog]
    {{ b1 }}[{{ item description }}]@{ assigned: '{{ agent }}', priority: 'Low' }
  todo[Todo]
    {{ t1 }}[{{ item description }}]@{ assigned: '{{ agent }}', priority: 'High' }
  wip[In progress]
    {{ w1 }}[{{ item description }}]@{ assigned: '{{ agent }}', priority: 'High' }
  blocked[Blocked]
    {{ x1 }}[{{ item description }}]@{ assigned: '{{ agent }}', priority: 'Very High', ticket: 'D-1' }
  review[Review]
    {{ r1 }}[{{ item description }}]@{ assigned: '{{ agent }}', priority: 'High' }
  done[Done]
    {{ d1 }}[{{ item description }}]@{ assigned: '{{ agent }}', priority: 'High' }
```

---

## Blocked cards → decisions

Every card in **Blocked** must point to an open item in `./DECISIONS.md`
so the human knows exactly what unblocks it.

| Card | Owner agent | Blocked on | Decision id |
| --- | --- | --- | --- |
| {{ item }} | {{ agent }} | {{ what }} | {{ D-x }} |
