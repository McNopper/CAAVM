# AI — the agent workspace

Everything in here is written and read by **agents**. A human never has to open this
folder; the PM agent distills all of it into the sibling `../Human/` folder. This
README documents how the workspace is organized and the naming conventions agents follow.

## Layout

```
AI/
├── README.md                     ← you are here
├── capabilities/                 ← pooled, on demand
│   ├── SKILL.md                  ← a reusable, goal-neutral method
│   └── MCP.md                    ← external tools / data / resources
└── agents/
    ├── pm/                       ← the ALWAYS-PRESENT PM agent (exactly one)
    │   ├── AGENT.md              ← identity + team designer + human interface
    │   ├── PLAN.md               ← the PM playbook
    │   ├── TASKS.md              ← the PM's own coordination board
    │   └── INBOX.md              ← worker issues bubble up here (PM triages)
    ├── _role-template/           ← the PM copies this to create each worker instance
    │   ├── AGENT.md · PLAN.md · TASKS.md
    │   └── runs/_run-template/RUN.md
    └── <role>-<NN>/              ← a spawned worker INSTANCE (see below)
```

## Agents, roles, and instances

- A **role** is a function the project needs (architect, developer, tester…).
- An **instance** is one running agent of that role. A role can have **several**
  instances working in parallel — the PM decides how many.
- Each instance gets its **own folder**, named `<role>-<NN>`:

  ```
  AI/agents/developer-01/
  AI/agents/developer-02/     ← a second developer, running at the same time
  AI/agents/architect-01/
  AI/agents/tester-01/
  ```

  The PM assigns each instance a **lane** (in its `AGENT.md`) so parallel instances
  of the same role don't collide.

## Dynamic run folders (date · time · instance)

Agents progress **autonomously** over many sessions. Every bounded working session
is a **run**, captured in a dynamic folder named by date and time, under the
instance that did the work:

```
AI/agents/<role>-<NN>/runs/<YYYY-MM-DD>T<HH-MM-SS>/RUN.md
                                                └ plus any scratch output for that session

# e.g.
AI/agents/developer-01/runs/2026-07-14T10-40-22/RUN.md
AI/agents/developer-01/runs/2026-07-14T14-05-08/RUN.md
AI/agents/developer-02/runs/2026-07-14T11-20-41/RUN.md
```

The path therefore encodes **agent (role), instance, date, and time** — a complete,
ordered, per-instance history. Use `runs/_run-template/RUN.md` as the starting point
for each new run.

> Naming rules: `<NN>` is a zero-padded counter per role (`01`, `02`, …).
> Timestamps use `YYYY-MM-DDTHH-MM-SS` (24-hour, hyphens instead of colons so the name
> is filesystem-safe). Never reuse or rewrite a past run folder — append a new one.

## The bubble-up rule

A worker instance never contacts the human. When it hits the edge of its autonomy it
appends to `agents/pm/INBOX.md`. The PM triages: it resolves most issues itself and
escalates to `../Human/DECISIONS.md` only what crosses the `../Human/BRIEF.md`
autonomy boundary. This keeps the human's side quiet and legible.

## What agents may edit

| Path | Worker instance | PM | Human |
| --- | --- | --- | --- |
| `../Human/BRIEF.md` | read | read | **write** |
| `../Human/ROADMAP.md` · `BOARD.md` · `STATUS.md` · `DECISIONS.md` | read | **write** | read (answers DECISIONS) |
| `AI/agents/pm/INBOX.md` | **append** | **write** | — |
| `AI/agents/<role>-<NN>/*` | **write** (own) | read | — |
| `AI/capabilities/*` | read | read | — |
