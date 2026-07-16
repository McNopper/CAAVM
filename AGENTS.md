# AGENTS.md — Hephaestus opencode workflow

## About this document
- **Kind:** `doc` / repo-level workflow convention (auto-loaded by opencode from the git root).
- **Read by:** any agent operating in this repo; **written by:** maintainers.
- **Related:** pairs with `README.md`; the authoritative tier→model mapping lives in the `pm-orchestrate-execution` skill.

Repository-level conventions for agentic work in this repository. Hephaestus is an
**opencode-native**, **domain-organized** system: skills and agents are flat under
`.opencode/` and named by `<domain>-<descriptor>`; project management is a concrete,
Scrum-like ticket/sprint workflow powered by the `pm` MCP server; C++ and graphics
tooling are first-class agents / MCP tools.

## Two scopes

- **A whole initiative / project** → the **PM agent** (`pm`) runs the Scrum workflow
  over tickets in the `pm` MCP server (one project per initiative; multiple projects
  coexist). The human is Product Owner: writes the brief/goal, prioritizes the backlog,
  accepts at Sprint Review. Issues bubble up to the PM and only human-worthy ones are
  escalated.
- **A software change inside a project** → a ticket of the right `role` is claimed by a
  worker, which uses the matching `software-*` / `test-software-*` skill, and records
  its artifact back on the ticket.

> **The PM/ticket system is optional.** Every skill and agent can be used **directly** by
> a human (or by another agent) with no ticket, sprint, or `pm` MCP server involved — for
> ad-hoc work, just invoke the skill or `/agents` you need. Use the PM system only when you
> want tracked, multi-agent, sprint-based execution. Likewise, `pm-doc-about` (and any
> skill) works standalone, independent of PM.

## Skills are discovered flat by domain

opencode loads every `SKILL.md` under `.opencode/skills/*/`. There are no subfolders by
domain — the **domain is in the name**. Naming convention: `<domain>-<descriptor>`.

| Domain prefix | Meaning | Example skills |
|---|---|---|
| `software-` | Definition (what/how) | `software-requirements`, `software-system`, `software-architecture`, `software-design`, `software-implementation` |
| `test-software-` | Verification of a definition level | `test-software-implementation`, `-design`, `-architecture`, `-system`, `-requirements` |
| `pm-` | Project management | `pm-operating-model`, `pm-orchestrate-execution`, `pm-route-request`, `pm-audit-traceability`, `pm-estimate-costs`, `pm-create-ticket` |
| `cpp-` | C++ execution utility | `cpp-tools` (methodology; the `cpp-tools` agent runs the commands) |
| `graphics-` | Graphics utility (thin) | `graphics-render-comparison` (the heavy lifting is the `mcp.graphics` tools) |

Cross-cutting coordination agents are **unprefixed** (`orchestrator`, `planner`,
`executor`, `reviewer`, `rubberduck`, `pm`, `cpp-tools`, `graphics-expert`).

## The ticket / sprint workflow (the PM)

The `pm` MCP server stores **tickets** and **sprints**, scoped per **project** so
several independent projects can run at once. States:

```
product-backlog --plan--> sprint-backlog --claim--> in-progress --verify--> in-review --accept--> done
   (incomplete on sprint close ──────────────────────────────────────────────────────┘)
blocked = orthogonal flag (blocked:bool + blocker:str) at any active state
```

- A worker **self-claims** by role: `pm_claim_ticket(role=…)` atomically finds the next
  matching ticket, moves it to `in-progress`, sets `assignee`. Two agents never get the
  same ticket. A returned ticket (`pm_release_ticket`) can be picked up by a *different*
  agent.
- When a worker produces an artifact (file, git commit/branch, doc), it records it with
  `pm_add_artifact(kind=file|git|path|url|doc, ref=…)` **before** moving to `in-review` —
  the ticket is the hand-off contract.
- Review/verification failure sends the ticket back to `in-progress` (rework loop).
- See `pm-operating-model` for Scrum events, the Definition of Done, and the
  bubble-up → escalation rule; `pm-create-ticket` for how to fill a ticket; `pm-route-request`
  when the next step is ambiguous; `pm-audit-traceability` for the definition→verification matrix.

## Canonical composition hierarchy (no V-model framing)

The dividing line is **reuse scope** (static-vs-shared linkage is a build decision):

- **Unit** → smallest implementation element with a clear interface.
- **Component** → composed of units; **internal** to this software (linked in).
- **Library** → composed of components; **independently deployable and reusable outside
  this software**; exposes a clear interface and dependency rules.
- **Software system** → composed of libraries plus external/system interfaces.
- **Package/folder** (and language *modules*) → organization only; not a lifecycle level.

Verification maps by level: `test-software-implementation` (unit) ↔ `software-implementation`,
`test-software-design` (component) ↔ `software-design`, `test-software-architecture` (library)
↔ `software-architecture`, `test-software-system` (integration) ↔ `software-system`,
`test-software-requirements` (acceptance) ↔ `software-requirements`.

## C++ and graphics are tools, not a separate lifecycle

- **C++**: the `cpp-tools` **agent** runs CMake configure/build, clang-format, cppcheck,
  clang-tidy via bash and reads their reports (methodology in the `cpp-tools` skill). The
  old `cpp/mcp` server is gone.
- **Graphics**: window capture, RenderDoc capture, and render comparison are **MCP tools**
  in `mcp.graphics` (`graphics_screenshot`, `graphics_renderdoc_capture`,
  `graphics_renderdoc_frame`, `graphics_compare_renders`). The `graphics-expert` agent
  (pinned to Opus) drives them for frontier-level graphics work; `graphics-render-comparison`
  is the thin methodology skill.

## Model tiers (model-neutral agents)

Agents and docs reference **tiers**, never hard-coded model IDs. The authoritative
tier→model mapping lives in `pm-orchestrate-execution`. All agents are model-neutral
**except** `graphics-expert`, which is pinned to Opus (`very-high`).

| Tier | Selection rule |
|---|---|
| `very-low` | cheapest/fastest for trivial, mechanical edits |
| `low` | best available open-weight model — **default executor** |
| `mid` | balanced general model for standard impl/tests |
| `high` | top-capability reasoning + large context — **planning + review** |
| `very-high` | frontier/highest-risk — **run twice & reconcile** (Opus) |

Tier-selection rule: pick the **lowest tier whose criteria still satisfy the task**;
escalate (never de-escalate) when uncertain.

## Custom agents (`.opencode/agent/`)

Lean, flat, model-neutral (except `graphics-expert`):

- **Coordination (unprefixed):** `orchestrator` (kicks off the sprint; workers self-claim),
  `planner` (high-tier plan + execution manifest), `executor` (open-tier task execution;
  records artifacts), `reviewer` (high-tier final review; edit-denied), `rubberduck`
  (cross-vendor critic; edit-denied), `pm` (Scrum Master + PO proxy; always present).
- **Domain agents:** `cpp-tools` (C++ execution), `graphics-expert` (Opus; graphics).

## opencode feature usage (recommended)

- Plan mode (`Tab`) for multi-file / multi-phase changes before implementation.
- `/agents` to select a coordination or domain agent.
- `/models` to pick a model for a task (tiers resolve here). Providers expected:
  **Z.AI** (GLM-5.2), **GitHub Copilot** (Opus 4.8), **OpenAI** (GPT-5.6).
- The `orchestrator` dispatches concurrent subagents (Task tool) for parallel tickets;
  workers self-claim the rest via `pm_claim_ticket`.
- Skills auto-load from `.opencode/skills/`; reference files with `@`.
- Run the project's verification gate (e.g. `cpp/` `verify` target) via bash before merge.
