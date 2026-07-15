<!--
  TEMPLATE · Worker agent instance · Lives in AI/
  Owner:     This instance (it writes and revises this itself)
  Path:      AI/agents/<role>-<NN>/PLAN.md
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# PLAN — {{ role }}-{{ NN }}

> This instance's **route**. It decomposes its lane of `../../../Human/ROADMAP.md`
> into a workable approach aimed at the goal in `../../../Human/BRIEF.md`. It revises
> this freely as it learns; it never edits the Human/ side. Work items live in
> `./TASKS.md`; each session is logged under `./runs/`.

## Anchors (read-only)

- **Goal & fence:** `../../../Human/BRIEF.md`
- **Time plan (PM-owned):** `../../../Human/ROADMAP.md`
- **Identity & limits:** `./AGENT.md`

## Mission (one sentence)

> {{ How this instance moves the project toward the goal }}

## Milestones / lane this instance owns

Each deliverable matures **iteratively** (draft → reviewed → accepted).

| Milestone / lane | Approach | Done when (brief success criterion) |
| --- | --- | --- |
| {{ item }} | {{ how }} | {{ criterion }} |

## Working approach

- **Decomposition:** milestones → tasks in `./TASKS.md`.
- **Session:** open a run folder `./runs/<date>T<HH-MM-SS>/`, work the loop, log RUN.md.
- **Loop:** pick top task → execute → self-check vs the brief → update `./TASKS.md`
  → bubble up any blocker in `../pm/INBOX.md`.
- **Definition of done (this instance):** {{ conditions; must honor non-goals }}

## Risks & blockers

| Risk / blocker | Impact | Mitigation | Bubble up? |
| --- | --- | --- | --- |
| {{ risk }} | {{ H/M/L }} | {{ action }} | {{ yes → ../pm/INBOX.md / no }} |
