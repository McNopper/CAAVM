<!--
  PM AGENT · Always present · The PM's own playbook
  Owner:     The PM agent
  Essence:   HOW the PM runs any project. Reusable playbook; fill project specifics.
-->

# PLAN — project manager / owner

> The PM's route for the whole project. It is the meta-plan: not a single role's
> work, but how the PM converts `../../../Human/BRIEF.md` into a running team and
> keeps it aligned to the goal.

## Anchors

- **Direction (human, read-only):** `../../../Human/BRIEF.md`
- **Identity & limits:** `./AGENT.md`
- **Owns:** `../../../Human/ROADMAP.md`, `../../../Human/BOARD.md`, `../../../Human/STATUS.md`

## Mission (one sentence)

> Deliver the `BRIEF.md` goal by fielding the right agents, sequencing their work,
> and keeping the human observing and in control.

## Step 1 — Decide the roles

From the goal and scope, choose the minimal set of roles. Record the reasoning.

| Role chosen | Why the goal needs it | Skip if… |
| --- | --- | --- |
| {{ role }} | {{ reason }} | {{ condition under which it's unnecessary }} |

> Common **development** roles: architect, developer, tester/QA, reviewer, devops.
> Common **planning / discovery** roles: researcher, analyst, designer, planner,
> writer. Mix freely — the goal decides the team. Only spawn what the goal actually
> requires.

## Step 2 — Spawn the instances

For each role, decide **how many instances** the workload needs, then copy
`../_role-template/` → `../<role>-<NN>/` (e.g. `../developer-01/`, `../developer-02/`),
and seed each `AGENT.md` (function, lane, boundaries, capabilities). Note the spawns
in `../../../Human/ROADMAP.md`.

## Step 3 — Build the plan

- **Roadmap (Gantt):** lay milestones on a timeline in `../../../Human/ROADMAP.md`.
- **Board (Kanban):** seed columns and initial cards in `../../../Human/BOARD.md`.

## Step 4 — Run & keep legible

Each PM loop:
- Pull each instance's `TASKS.md` state → update `../../../Human/BOARD.md` (Kanban).
- Advance/adjust the Gantt in `../../../Human/ROADMAP.md` as reality shifts.
- Rewrite `../../../Human/STATUS.md` in plain language for the human.

## Step 5 — Triage bubbled-up issues

For every open item in `./INBOX.md` (worker bubble-ups):
- **Resolve** if within PM autonomy (reassign, resequence, unblock, spawn/retire an instance).
- **Escalate** to `../../../Human/DECISIONS.md` if it touches the `BRIEF.md` autonomy boundary.
- Reflect the outcome in the roadmap/board/status.

## Definition of done (PM)

- Every `BRIEF.md` success criterion is met and shown green in `../../../Human/STATUS.md`.
- `./INBOX.md` and `../../../Human/DECISIONS.md` have no orphaned open items; the
  human signed off at the final check-in.

## Risks & blockers

| Risk / blocker | Impact | Mitigation | Escalate? |
| --- | --- | --- | --- |
| {{ risk }} | {{ H/M/L }} | {{ action }} | {{ yes → Human/DECISIONS.md / no }} |
