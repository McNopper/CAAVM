<!-- EXAMPLE (filled) · PM agent · The PM's playbook for Snip -->

# PLAN — project manager / owner

> How the PM is running Snip end to end.

## Anchors

- **Direction (read-only):** `../../../Human/BRIEF.md`
- **Identity & limits:** `./AGENT.md`
- **Owns:** `../../../Human/ROADMAP.md`, `../../../Human/BOARD.md`, `../../../Human/STATUS.md`

## Mission (one sentence)

> Ship Snip to staging within 6 weeks by coordinating architect → developer → tester
> and keeping Dana informed and in control.

## Step 1 — Roles & instances decided ✅

architect ×1, developer ×**2** (parallel write/read lanes), tester ×1 (rationale in
`./AGENT.md`). No devops/reviewer agent.

## Step 2 — Instances spawned ✅

`../architect-01/`, `../developer-01/`, `../developer-02/`, `../tester-01/` created
and seeded from `../_role-template/`.

## Step 3 — Plan built ✅

- Gantt in `../../../Human/ROADMAP.md` (Design → parallel Build → Verify → Ship).
- Kanban seeded in `../../../Human/BOARD.md`.

## Step 4 — Running (current)

Keeping the board and `../../../Human/STATUS.md` current each loop. Triaging
`./INBOX.md`. Overall health 🟡 because of one escalated decision (D-1).

## Step 5 — Triage

- **I-2, I-3 (resolved):** two-developer split and shared-schema ownership — decided
  internally, see `./INBOX.md`.
- **D-1 (escalated):** open-redirect policy raised by architect-01; blocks
  architect-01 + tester-01 + developer-02. Beyond the BRIEF autonomy boundary (a
  security posture choice) → **escalated to Dana** in `../../../Human/DECISIONS.md`.
  Recommended option C. Awaiting her call.

## Definition of done (PM)

- All five BRIEF success criteria green in `../../../Human/STATUS.md`; Dana approves staging deploy.

## Risks & blockers

| Risk / blocker | Impact | Mitigation | Escalate? |
| --- | --- | --- | --- |
| D-1 unresolved stalls design + security | H | Escalated; recommended C for launch | yes → Human/DECISIONS.md |
| 6-week deadline vs latency hardening | M | Two parallel devs; front-load load testing | no |
