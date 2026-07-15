# AGENTS.md — ToDo (Agentic Project Planning & Software Development)

Quick-start context for AI agents operating **inside** this `ToDo/` system, so the
rules of engagement don't have to be rediscovered each session.

## What this is

`ToDo/` is a **human-directed, agent-executed operating model** for both **project
planning and software development**. It is a set of reusable markdown templates that frame
goals, plan work, spawn roles, execute them, and close out. A human sets direction and
observes; a permanent **PM agent** plans and coordinates; **worker agents** do the work.

> Read `ToDo/README.md` for the full model and `ToDo/example/` for a filled instance.

## The one rule that matters: Human/ vs AI/

```
ToDo/
├── Human/   ← the ONLY folder a human reads. Enriched: Gantt, Kanban, dashboard.
└── AI/      ← the agent workspace. Terse, structured, machine-facing.
```

- **A human reads only `Human/`.** Keep it legible and current.
- **Agents work in `AI/`.** Do not make a human open `AI/` to understand status.

## Who writes what (do not violate)

| Path | Worker instance | PM | Human |
|------|-----------------|----|-------|
| `Human/BRIEF.md` | read | read | **write** (mandate — agents never edit) |
| `Human/ROADMAP.md` · `BOARD.md` · `STATUS.md` · `DECISIONS.md` | read | **write** | read (answers DECISIONS) |
| `AI/agents/pm/INBOX.md` | **append** | **write/triage** | — |
| `AI/agents/<role>-<NN>/*` | **write** (own bundle) | read | — |
| `AI/capabilities/*` | read | read | — |

## Roles, instances, runs

- **PM agent** (`AI/agents/pm/`) is **always present**, exactly one. It reads
  `Human/BRIEF.md`, decides which roles + how many instances, spawns them from
  `AI/agents/_role-template/`, and owns the roadmap/board/status/decisions.
- **Instances:** one folder per running agent — `AI/agents/<role>-<NN>/`
  (e.g. `developer-01`, `developer-02`). A role can have several in parallel; the PM
  gives each a **lane** so they don't collide.
- **Runs:** each autonomous session gets a **second-precise** dynamic folder:
  `AI/agents/<role>-<NN>/runs/<YYYY-MM-DD>T<HH-MM-SS>/RUN.md`. Append-only — never
  rewrite a past run.

## The bubble-up rule (issues flow to the PM, never straight to the human)

1. A worker hits the edge of its autonomy → appends to `AI/agents/pm/INBOX.md`.
2. The PM triages: resolves it internally **or** escalates it to
   `Human/DECISIONS.md` — but only if it crosses the autonomy boundary in
   `Human/BRIEF.md` (scope/goal change, spend, irreversible action, security posture).
3. The human answers `Human/DECISIONS.md`; the PM applies the outcome.

If `Human/DECISIONS.md` is empty, the human can just watch `Human/STATUS.md`.

## The loop (built in — do not invent a process)

Waterfall once at the top (human writes `BRIEF.md`; PM writes the plan) → each
deliverable matures iteratively (draft → reviewed → accepted) → agile timeboxed runs
per instance → PM rolls progress up to `Human/STATUS.md`. Planning and development use
the **same** loop.

## Conventions

- **Never** edit `Human/BRIEF.md` — it is the human-only mandate.
- The PM writes the other `Human/` files; keep them enriched (Gantt/Kanban/dashboard)
  and plain-language.
- Worker bundles stay terse and structured; put run detail in `runs/…/RUN.md`.
- Timestamps: `YYYY-MM-DDTHH-MM-SS` (24-hour, hyphens — filesystem-safe, sortable).
- One in-flight task per instance; bubble blockers up rather than stalling silently.

## Gotchas

- **This is `ToDo/`, a self-contained model** — it does not build or ship with the
  Hyperion renderer. Nothing here touches CMake, Vulkan, or the `src/` tree.
- **Instance ids are folder-encoded** (`developer-02`), and **run ids are
  time-encoded to the second**. Reusing a run folder loses history — always append a
  new one.
- **Escalate up, not out.** A worker never writes to `Human/`; only the PM does.
