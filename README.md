# 🔱 Hephaestus

## About this document
- **Kind:** `doc` / repo README (top-level entry point)
- **Read by:** humans evaluating/adopting the template; **written by:** maintainers
- **Related:** pairs with `AGENTS.md` (workflow conventions) and the skill/agent set under `.opencode/`

> *[Hephaestus](https://en.wikipedia.org/wiki/Hephaestus) — Greek god of the forge, and the one who built automatons: Talos, the golden mechanical attendants.*

## Overview

Hephaestus is an **opencode-native** template for **agentic project management and
software development**. It is organized by **domain** (not by a lifecycle or folder
tree): skills and agents are flat under `.opencode/` and named `<domain>-<descriptor>`.
Project management is a concrete, Scrum-like **ticket/sprint** workflow powered by the
`pm` MCP server; C++ and graphics are first-class **tools** (an agent and an MCP server),
not a separate lifecycle.

Three ideas hold it together:

- **Domains in names, not folders.** opencode discovers every `SKILL.md` under
  `.opencode/skills/*/`. Naming convention `<domain>-<descriptor>`
  (`software-`, `test-software-`, `pm-`, `cpp-`, `graphics-`); coordination agents are
  unprefixed.
- **A concrete PM, not a metaphor.** The `pm` agent runs Scrum over tickets in the `pm`
  MCP server. Tickets carry a `role` (discipline), and workers **self-claim** by role
  (`pm_claim_ticket`). Multiple independent **projects** coexist in one server.
- **Model-neutral by default.** Agents reference a *tier*; the concrete model
  resolves from `opencode.json` (default) and any per-agent overrides. Only `graphics-expert`
  is pinned (to `very-high`).

> **The PM/ticket system is optional.** Any skill or agent can be used **directly** by a
> human (or another agent) with no ticket or sprint — just invoke the skill or pick an
> agent with `/agents`. The PM system is there when you want tracked, multi-agent, sprint
> execution; skip it for ad-hoc work. Skills like `pm-doc-about` also work standalone,
> independent of PM.

## Layout

| Path | What it is |
|---|---|
| `opencode.json` (repo root) | project config — default `model`, `AGENTS.md`, and the `pm` + `graphics` MCP servers. |
| `AGENTS.md` (repo root) | opencode-first workflow conventions and routing. |
| `.opencode/skills/*/SKILL.md` | the skill library, flat by domain. |
| `.opencode/agent/*.md` | lean custom agents (coordination + domain). |
| `.opencode/docs/` | `domains.md`, `contracts.md`. |
| `mcp/pm/`, `mcp/graphics/` | reusable MCP servers (project-scoped state, locking). |
| `mcp/base/locking_store.py` | shared concurrency-safe JSON store used by the `pm` server (a module, not a server). |
| `cpp/` | standalone AI-first C++23 build skeleton (its own `AGENTS.md`). |

## Skills (flat, by domain)

| Domain | Skills |
|---|---|
| `software-` (definition) | `software-requirements`, `software-system`, `software-architecture`, `software-design`, `software-implementation` |
| `test-software-` (verification) | `test-software-implementation`, `-design`, `-architecture`, `-system`, `-requirements` |
| `pm-` (project management) | `pm-operating-model`, `pm-orchestrate-execution`, `pm-route-request`, `pm-audit-traceability`, `pm-estimate-costs`, `pm-create-ticket`, `pm-doc-about` |
| `cpp-` (C++ utility) | `cpp-tools` (methodology; the `cpp-tools` agent runs the commands) |
| `graphics-` (graphics utility) | `graphics-render-comparison` (the heavy lifting is the `mcp.graphics` tools) |
| `code-` (code analysis) | `code-dependency` (package/namespace dependency map → Mermaid block diagram), `code-licenses` (third-party license audit → compatibility table + remediation) |

Verification maps by composition level: `test-software-implementation` ↔ `software-implementation`
(unit), `test-software-design` ↔ `software-design` (component), `test-software-architecture`
↔ `software-architecture` (library), `test-software-system` ↔ `software-system` (integration),
`test-software-requirements` ↔ `software-requirements` (acceptance).

### Terminology (canonical in this repo)

The dividing line between **component** and **library** is **reuse scope**, not size and
not static-vs-shared linkage (that is a build decision):

| Term | Meaning | Reuse scope | Composes into |
|---|---|---|---|
| **Unit** | Smallest element with a clear interface; implementation fills its content. | within one component | Component |
| **Component** | Units behind a clear interface; **internal** to this software. | within this software | Library |
| **Library** | Components behind a clear interface; **reusable outside this software**. | reusable across systems | Software System |
| **Software System** | Integrated product of libraries + external interfaces. | the deliverable | — |
| **Package/Folder** | Organization only; a language *module* is also just organization. | — | — |

## The ticket / sprint workflow

The `pm` MCP server stores **tickets** and **sprints**, scoped per **project** so several
independent projects run at once. Ticket states:

```
product-backlog --plan--> sprint-backlog --claim--> in-progress --verify--> in-review --accept--> done
   (incomplete on sprint close ───────────────────────────────────────────────────────────────┘)
blocked = orthogonal flag (blocked:bool + blocker:str) at any active state
```

Key rules:

- **Self-claim by role.** A worker loops `pm_claim_ticket(role=…)`; the call is atomic so
  two agents never get the same ticket. A returned ticket (`pm_release_ticket`) can be
  picked up by a *different* agent.
- **Record artifacts.** When a worker produces a file, git commit/branch, or doc, it
  records it with `pm_add_artifact(kind=file|git|path|url|doc, ref=…)` *before* moving to
  `in-review` — the ticket is the hand-off contract.
- **Rework loop.** Review/verification failure returns the ticket to `in-progress`.
- **Bubble-up → escalation.** A blocked worker sets `blocked` + a `blocker`; the PM resolves
  internally or escalates only human-worthy decisions.

See `pm-operating-model` (Scrum events, DoD, escalation), `pm-create-ticket` (how to fill
a ticket), `pm-route-request` (ambiguous next step), `pm-audit-traceability` (matrix).

## Agents (lean, flat, model-neutral except one)

| Agent | Role | Model |
|---|---|---|
| `orchestrator` | kicks off the sprint; workers self-claim | tier (`high`) |
| `planner` | high-tier plan + execution manifest | tier (`high`) |
| `executor` | open-tier task execution; records artifacts | tier (`low`) |
| `reviewer` | high-tier final review (edit-denied) | tier (`high`) |
| `rubberduck` | cross-vendor critic (edit-denied) | tier (different vendor) |
| `pm` | Scrum Master + PO proxy; always present | tier (`high`) |
| `cpp-tools` | C++ build/format/static-analysis via bash | tier (`low`) |
| `graphics-expert` | frontier graphics work; drives `mcp.graphics` | **pinned `very-high`** |

## C++ and graphics

- **C++**: the `cpp-tools` *agent* runs CMake configure/build, clang-format, cppcheck,
  clang-tidy and reads their reports (methodology in the `cpp-tools` skill). The old
  `cpp/mcp` server is gone — C++ is an agent now.
- **Graphics**: `mcp.graphics` exposes `graphics_screenshot`, `graphics_renderdoc_capture`,
  `graphics_renderdoc_frame`, `graphics_compare_renders`. `graphics-expert` (very-high tier) drives
  them; `graphics-render-comparison` is the thin methodology skill.

## Model tiers

Agents/docs reference **tiers**, never hard-coded model IDs. The concrete model
behind each tier is configured in `opencode.json` (the default `model` field)
and in any per-agent override (only `graphics-expert` overrides, pinning to
`very-high`); resolve through `/models`.

| Tier | Selection rule |
|---|---|
| `very-low` | cheapest/fastest for trivial, mechanical edits |
| `low` | best open-weight model — **default executor** |
| `mid` | balanced general model for standard impl/tests |
| `high` | top-capability reasoning + large context — planning + review |
| `very-high` | frontier/highest-risk — run twice & reconcile |

## Recommended opencode workflow

1. **Frame the project:** the human writes the brief/goal; the `pm` agent creates tickets
   (`pm_create_ticket`) in `product-backlog`.
2. **Sprint planning:** `pm_plan_sprint` commits tickets to a sprint (`sprint-backlog`).
3. **Execute:** workers `pm_claim_ticket(role=…)`, use the matching `software-*` /
   `test-software-*` skill, record artifacts, and move tickets to `in-review`.
4. **Review & accept:** `reviewer` / test skills verify; the `pm` agent accepts → `done`.
5. **Iterate:** defects rework; `pm_close_sprint` returns unfinished tickets to the backlog.

Use **Plan mode** (`Tab`) for multi-file changes; `/agents` to pick an agent; `/models` to
resolve a tier; the `orchestrator` dispatches parallel subagents. Skills auto-load from
`.opencode/skills/`; reference files with `@`.

## Install & Use (opencode)

1. [Install opencode](https://opencode.ai/docs/) (e.g. `npm install -g opencode-ai`).
2. Connect providers via `/connect` (e.g. Z.AI, GitHub Copilot, OpenAI —
   whichever you use).
3. Install MCP deps: `pip install -r mcp/pm/requirements.txt -r mcp/graphics/requirements.txt`.
4. Run `opencode` from this repo. Skills, agents, and `AGENTS.md` auto-load; the `pm` and
   `graphics` MCP servers start from `opencode.json`.

> **MCP scope:** the bundled `pm` and `graphics` servers implement a deliberately
> minimal stdio JSON-RPC loop (`initialize`, `tools/list`, `tools/call`). They expose
> the tools listed above; they do not implement `resources`, `prompts`, cancellation,
> or progress. That is sufficient for opencode tool calls.

> **Local plugin deps:** `.opencode/` carries a local `.opencode/package.json`
> (`@opencode-ai/plugin`) that is **git-ignored** along with its `node_modules` — it is
> a per-clone convenience, not part of the template. A fresh clone starts without it.

### Reuse as a template

Hephaestus is a **template repo**. Copy the pieces you need:

```bash
# from your project root
mkdir -p .opencode/skills .opencode/agent mcp
cp -R /path/to/Hephaestus/.opencode/skills/* .opencode/skills/
cp -R /path/to/Hephaestus/.opencode/agent/*  .opencode/agent/
cp -R /path/to/Hephaestus/mcp/*               mcp/
cp    /path/to/Hephaestus/opencode.json .
cp    /path/to/Hephaestus/AGENTS.md .
```

Trim to what you need (e.g. drop `graphics-*` / `mcp.graphics` if unused). Set the
default `model` in `opencode.json` (and any per-agent overrides) to match your
providers.

### What *not* to do

- ❌ Don't hard-code a model in an agent beyond the default — reference a **tier**; only
  `graphics-expert` pins a model.
- ❌ Don't rename `SKILL.md` or rely on the folder name — identity is the front-matter
  `name:` (must match its folder).
- ❌ Don't put two `AGENTS.md` in the same folder — opencode loads one per git-root/cwd.
- ⚠️ Skills in `.opencode/skills/` load for everyone who runs `opencode` here — commit only
  what the project needs.

## C++ build template

[`cpp/`](cpp/) is a standalone **AI-first C++23 build template** (its own `CMakeLists.txt`,
`CMakePresets.json`, `AGENTS.md`, `.clang-tidy`, `.clang-format`, `src/`, `include/`,
`tests/`). It emits machine-readable reports (compile DB, Doxygen XML, clang-tidy /
cppcheck exports) and runs `verify` (fast) and `verify-full` (strict). The `cpp-tools`
agent drives it. See [`cpp/README.md`](cpp/README.md) and [`cpp/AGENTS.md`](cpp/AGENTS.md).

Presets ship for two toolchains: `default`/`release`/`analysis` (Ninja + Clang,
which enable the full AI analysis stack) and **`windows`** (Visual Studio + MSVC,
which builds and tests but skips the Clang-based analysis by design). On a Windows
host without Clang/Ninja, use `cmake --preset windows`.

## License

[MIT](LICENSE) © 2026 Norbert Nopper.

### Third-party licenses

The bundled MCP servers depend on **filelock** (Unlicense), **Pillow** (HPND)
and **numpy** (BSD-3-Clause) — all permissive and compatible with MIT. Their
full license texts and copyright notices are in
[`THIRD-PARTY.md`](THIRD-PARTY.md). The local opencode Node plugin
(`.opencode/`, git-ignored) and the `cpp/` template's test-only GoogleTest
(BSD-3-Clause, fetched on demand) are not redistributed and are documented there
as well.
