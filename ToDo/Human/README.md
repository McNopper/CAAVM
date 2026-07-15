# Human — your side of the project

**You only need this folder.** Everything an agent does happens in the sibling
`../AI/` workspace; the PM agent keeps this folder up to date for you. If you never
open `../AI/`, you will still have the full picture.

## The five files here

| File | What it is | Who writes it | When you touch it |
| --- | --- | --- | --- |
| **`BRIEF.md`** | Your mandate: customers, goal, scope, and the autonomy boundary. | **You** | Once, up front — and whenever you change direction. |
| **`ROADMAP.md`** | The time plan as a **Gantt** + a plain-language summary. | PM agent | Read anytime; it's kept current. |
| **`BOARD.md`** | The live **Kanban** of what every agent is working on. | PM agent | Glance when you want detail. |
| **`STATUS.md`** | Your **dashboard** — health, progress, what needs you. | PM agent | Your default screen. |
| **`DECISIONS.md`** | Only the choices that need **you**. | PM agent (you answer) | When it's non-empty. |

## How to use it

1. **Write `BRIEF.md`** and hand the project to the PM agent (`../AI/agents/pm/`).
2. **Watch `STATUS.md`.** It tells you if things are 🟢 on track, 🟡 at risk, or
   🔴 blocked, and whether anything is waiting on you.
3. **Answer `DECISIONS.md`** when the PM escalates something past your autonomy
   boundary. Everything smaller, the PM handles without bothering you.

That's the whole loop: you direct and observe; the agents execute.

## Why you don't need the AI folder

The agents produce a lot of working detail — plans, task boards, timestamped run
logs, one folder per agent instance. That all lives in `../AI/` and is optimized for
machines. The PM is the **translation layer**: it distills all of it into the five
human-readable files above. Peek into `../AI/` only if you're curious.
