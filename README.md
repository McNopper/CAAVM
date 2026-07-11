# 🔱 Hephaestus

> *[Hephaestus](https://en.wikipedia.org/wiki/Hephaestus) — Greek god of the forge, and the one who
> built automatons (Talos, the golden mechanical attendants). A fitting patron for a disciplined,
> agent-driven build process.*

## Overview

Hephaestus is an **opencode** template with three layers that work together:

- **V-model skills (10)** — a paired definition/verification software lifecycle.
- **On-demand utility skills (8)** — routing, traceability, plan orchestration, cost
  estimation, C++ template, and graphics helpers, triggered only when relevant.
- **Custom agents (23)** in `.opencode/agent/` — 5 role + 10 lifecycle + 8 utility agents
  that operationalize an **autonomous, agile, budget-aware** workflow: a high-tier model
  plans, an open model executes subdivided tasks, a high-tier model reviews, and an
  `orchestrator` drives the fleet — revisiting V-model steps iteratively until convergence.

Model choice is **rule-based, not hard-coded**: tasks reference a *tier*, and the concrete
model is resolved from a single authoritative mapping (see
[Model-tier tags](#model-tier-tags-used-by-software-plan-orchestration)).

## Skills

Hephaestus provides ten **V-model lifecycle** skills arranged as a software lifecycle. The
left side defines the software; the right side verifies it. Each definition skill is
paired with exactly one verification skill, and every skill is scoped so it does not
overlap its neighbours. The skills are deliberately kept at a lean **hobby-project**
level — minimal but useful; a heavier production variant could live as a separate set.

| Slot | Definition (left)              | ↔ | Verification (right)              | Slot |
|------|--------------------------------|---|----------------------------------|------|
| 01   | `software-requirements`        | ↔ | `software-acceptance-test`        | 10   |
| 02   | `software-system`              | ↔ | `software-integration-test`       | 09   |
| 03   | `software-architecture`        | ↔ | `software-library-test`           | 08   |
| 04   | `software-design`              | ↔ | `software-component-test`         | 07   |
| 05   | `software-implementation`      | ↔ | `software-unit-test`              | 06   |

Each skill lives in `.opencode/skills/<name>/SKILL.md` and follows the same lean template:
V-model position, hobby-level scope, core principles, a compact default output, and
hand-off guidance to neighbouring skills.

## Terminology (canonical in this repo)

These terms are used consistently across all lifecycle skills:

The dividing line between **component** and **library** is **reuse scope**, not
size and not static-vs-shared linkage (that is a build decision):

| Term | Meaning | Reuse scope | Composes into |
|---|---|---|---|
| **Unit** | Smallest structural element with a clear interface (class/type/operation); implementation fills in its final content. | within one component | Component |
| **Component** | Building block composed of units behind a clear interface; **internal** to this software (linked in). | within this software only | Library |
| **Library** | Independently deployable element composed of components behind a clear interface; **reusable outside this software** too. Built as a static *or* shared library — that is a build decision. | reusable across software systems | Software System |
| **Software System** | Full integrated product composed of libraries plus external/system interfaces. | the deliverable | — |
| **Software** | Umbrella term; in lifecycle decisions prefer the explicit term **software system**. | — | — |
| **Package/Folder** | Organization mechanism only (layout/namespacing); a language *module* (C++/Python/Rust/Go) is likewise just code organization, not a lifecycle level. | — | — |

### Terminology defensibility note

This wording is intentionally practical and standards-informed:

- **Unit** and **component** follow ISO/IEC/IEEE 24765 (SEVOCAB) and Automotive
  SPICE (ASPICE): a *component* is a collection of *units* behind a defined
  interface; a *unit* is the smallest separately testable element.
- **Library** is used instead of "module" deliberately. ASPICE has no "module"
  level, ISO/IEC/IEEE 29119 treats "module testing" merely as a *synonym* of
  component testing, and the word *module* means something different in almost
  every language (C++ compilation unit, Python file, Go versioned dependency,
  Rust namespace). "Library" cleanly captures "independently deployable, reusable
  outside this software".
- Note the classic trap: in Component-Based SE (Szyperski/UML) a *component* is
  defined as an independently deployable, third-party-reusable unit — i.e. what
  this repo calls a **library**. We reserve *component* for the internal level and
  *library* for the externally reusable level to keep the two distinct.
- **Package/folder** (and a language *module*) are organizational only;
  composition is unit → component → library → software system.

## opencode workflow (recommended)

1. **Start with intent, not implementation:** "Use `software-requirements` to capture goals and acceptance criteria."
2. **Move down the left side:** requirements → system → architecture → design → implementation.
3. **Verify across the right side:** unit/component/library/integration/acceptance tests.
4. **Use explicit hand-offs:** ask opencode to "handoff to `<next-skill>`" when outputs are ready.
5. **Switch modes:** **Plan mode** (`Tab`) to plan a change, **Build mode** to execute it.
6. **For C++ work in this repo:** invoke `cpp-template-workflow` so tasks use `cpp/` and its canonical commands.

### opencode features worth using here

| Feature | Why it matters in this repo |
|---|---|
| **Plan mode** (`Tab`) | Plan multi-file/multi-phase changes before implementation; switch to Build to execute. |
| `/agents` | Select a role/lifecycle/utility custom agent from `.opencode/agent/`. |
| **Task/subagent tool** | The `orchestrator` dispatches parallel groups as concurrent subagents; independent skills/docs can be edited in one pass. |
| `/models` + `/connect` | Pick the model per task (tiers resolve here); add the providers this repo expects — Z.AI, GitHub Copilot, OpenAI. |
| `@` references | Fuzzy-search files to add precise context (`@opencode/skills/...`). |
| `/init` | (Re)generate the repo `AGENTS.md` from the project structure. |
| `/undo` + `/redo` | Revert and re-apply a change when a result isn't what you wanted. |
| `/share` | Share the current conversation link with the team. |

### Prompting pattern

Use concrete phrasing so skill matching is deterministic:

- "Use the `software-system` skill to define major parts, storage, and external interfaces."
- "Use the `software-library-test` skill to verify architecture boundaries and dependency rules."
- "Use the `software-vmodel-navigation` skill to route this request to the correct lifecycle step."

> Invoke skills by their front-matter `name` (e.g. `software-system`), which is also the
> folder name (opencode requires `name` to match the directory). The **Slot** numbers above
> are just the V-model lifecycle ordering, not part of any folder or skill name.

### On-demand skills outside the V-model

These use a similar lean **on-demand utility template** (closer to the graphics
skills than to the lifecycle template) but are triggered when needed:

| Skill | Purpose |
|---|---|
| `graphics-window-screenshot` | Capture only the rendered **client area** of a window (no title bar/borders). |
| `graphics-renderdoc-profiling` | GPU frame **capture & profiling** via RenderDoc CLI (`renderdoccmd`). |
| `graphics-render-comparison` | **Compare renderings** from different methods (diff images + PSNR/SSIM/FLIP). |
| `software-vmodel-navigation` | Route ambiguous requests to the correct V-model skill and produce a hand-off prompt. |
| `software-traceability-audit` | Build/audit traceability from requirements to tests across the V-model. |
| `software-plan-orchestration` | Review/rubberduck a plan, subdivide tasks for open-model execution, order by dependency, tag rule-based tiers, and drive autonomous execution via opencode. |
| `software-cost-estimation` | Price an execution plan/manifest before a run (per-task tokens × per-tier rate card, double-runs, retries) with de-escalation suggestions. |
| `cpp-template-workflow` | Automatically route C++ implementation/verification work through the `cpp/` template and canonical command targets. |

#### Model-tier tags (used by `software-plan-orchestration`)

Each ordered task is tagged with one tier. **Model selection is rule-based, not
hard-coded:** the tier is defined by a *selection rule*. Concrete model IDs are only the
current mapping **as of today** and live in a **single authoritative table** in
`software-plan-orchestration` — this doc lists only the durable rules and points there.

**GLM-5.2 is the main agent for everything** (all normal work — planning, execution,
review). **Opus 4.8** is used *only* when a task is too complex for GLM (`very-high`).
**GPT-5.6** rubberducks / cross-checks Opus when an independent review is required. All
three are ordinary opencode providers (connected via `/connect`), and the whole pipeline
runs at **1M-token context**.

| Tier tag | Selection rule (durable) |
|---|---|
| `very-low` | cheapest/fastest model adequate for trivial, mechanical edits |
| `low` | best available **open-weight** model — **default executor** |
| `mid` | balanced general model for standard impl/tests |
| `high` | top-capability reasoning + large context — **planning + review** |
| `very-high` | task too complex for GLM-5.2 — **Opus 4.8, run twice & reconcile** |
| (rubberduck) | cross-vendor review of Opus — **GPT-5.6** |

**Tier-selection rule:** GLM-5.2 is the default for every task; tag `very-high` (Opus)
only when GLM-5.2 demonstrably cannot do it. **Plan → Execute → Review:** GLM-5.2 plans;
GLM-5.2 runs each subdivided task; GLM-5.2 reviews at the end; GPT-5.6 rubberducks Opus.
Tasks are always **subdivided so an open model can execute each one**. The model IDs to
swap in over time live only in the authoritative mapping in
`.opencode/skills/software-plan-orchestration/SKILL.md`.

**Agile, budget-driven & reviewable.** The V-model runs **agile/iteratively**: objectives
can change and the plan/**execution manifest** is a *living* artifact that is amended (not
restarted). You can set a spend cap (e.g. "~$X today"); `software-cost-estimation` prices
the manifest and the orchestrator de-escalates/defers tasks to fit and halts at the cap.
All automation artifacts are **AI-first yet human-reviewable** — structured, stable YAML/
Markdown that both agents and people can read, diff, and revise.

### Custom agents (`.opencode/agent/`)

The workflow is operationalized by **23 custom agents** (opencode
[custom agents](https://opencode.ai/docs/agents/) — markdown files with YAML front matter in
`.opencode/agent/*.md`). Select one with **`/agents`**. Agents are **model-neutral** — they
reference a **tier**, and the concrete model is resolved from the authoritative mapping in
`software-plan-orchestration` at dispatch, so model choice stays dynamic and central.

| Group | Agents | Purpose |
|---|---|---|
| **Role (5)** | `orchestrator`, `planner`, `executor`, `reviewer`, `rubberduck` | Coordinate the fleet, plan, execute (open model), review (high), critique (cross-vendor, for Opus). |
| **V-model (10)** | `software-requirements` … `software-acceptance-test` | Thin wrappers delegating to the matching lifecycle `SKILL.md`. |
| **Utilities (8)** | `software-vmodel-navigation`, `software-traceability-audit`, `software-plan-orchestration`, `software-cost-estimation`, `cpp-template-workflow`, `graphics-window-screenshot`, `graphics-renderdoc-profiling`, `graphics-render-comparison` | Wrappers for the on-demand utility skills. |

The `orchestrator` runs the system **autonomously and harmonized**: it invokes a
skill/agent **only when its trigger applies** (`graphics-*` and `cpp-template-workflow`
stay dormant unless needed), verifies every task, merges parallel results, and treats the
V-model as **iterative** — a failed test/review re-opens the paired left-side step and
re-runs downstream until convergence.

## Install & Use (opencode)

The skills follow the portable [agent skills](https://opencode.ai/docs/skills/) format
(`SKILL.md` with `name` + `description` YAML front matter). opencode auto-loads skills,
agents, commands and config from the project's `.opencode/` directory (and walks up to the
worktree root for `opencode.json` / `AGENTS.md`).

### Option A — use them in this repo (zero install)

1. [Install opencode](https://opencode.ai/docs/) (e.g. `npm install -g opencode-ai`).
2. Connect the providers this repo expects — run `/connect` and add **Z.AI** (GLM-5.2),
   **GitHub Copilot** (Opus 4.8), and **OpenAI** (GPT-5.6).
3. Run `opencode` from this repository. Skills auto-load from `.opencode/skills/`,
   agents from `.opencode/agent/`, and `AGENTS.md` from the repo root. Confirm
   providers/models with `/models` and pick an agent with `/agents`.

This is the fastest way to use or develop the skills.

### Option B — install globally for every project

Copy (or symlink) the skill **and** agent folders into your personal opencode dirs:

```bash
# macOS/Linux — symlink so they stay in sync with this repo
ln -s "$(pwd)"/.opencode/skills/* ~/.config/opencode/skills/
ln -s "$(pwd)"/.opencode/agent/*  ~/.config/opencode/agent/
```

```powershell
# Windows PowerShell — copy the folders
Copy-Item -Recurse .\.opencode\skills\* "$env:USERPROFILE\.config\opencode\skills\"
Copy-Item -Recurse .\.opencode\agent\*  "$env:USERPROFILE\.config\opencode\agent\"
```

- Personal skills dir: `~/.config/opencode/skills/`
- Personal agents dir: `~/.config/opencode/agent/`
- Project config: `./opencode.json` (or `.opencode/opencode.json`)
- Recommended for global installs: include only the generic lifecycle/utility skills.
  Keep `cpp-template-workflow` project-scoped unless the target repo also contains
  this same `cpp/` template layout.

### Managing skills & agents in a session

- `/models` — list/choose loaded models.
- `/agents` — select a custom agent from `.opencode/agent/` for the session.
- `/connect` — add a provider (Z.AI, GitHub Copilot, OpenAI, …).
- `/init` — (re)generate `AGENTS.md` from the project structure.
- `@` — fuzzy-reference a file in your prompt.

**Triggering:** skills are invoked automatically by matching your request against each
skill's `description`. You can also name skills explicitly (recommended for precision),
using the front-matter `name` (e.g. `software-system`), not the folder label.

## Reuse in your own project (template repo)

Hephaestus is a **template repo** — you can adopt the whole thing or cherry-pick parts.

**A. Start a new repo from it** — if this repo is marked as a GitHub template, click
**"Use this template"** (otherwise fork or clone).
Your new repo already has `.opencode/skills/`, `.opencode/agent/` and `AGENTS.md`, so
opencode auto-loads the skills and exposes the agents (`/agents`) the moment you run
`opencode` in it.

**B. Add it to an existing project** — copy the pieces you want into your repo:

| Copy this | Into your repo at | Gives you |
|---|---|---|
| `.opencode/skills/*` | `.opencode/skills/` | the V-model + on-demand skills (auto-loaded as project skills) |
| `.opencode/agent/*` | `.opencode/agent/` | the role/lifecycle/utility custom agents (invoked with `/agents`) |
| `opencode.json` | repo root | project config (default model, instructions) |
| `AGENTS.md` | repo root | repo-level workflow/routing conventions (auto-loaded from git root & cwd) |
| `cpp/` contents | your C++ project's root **or** a subdir | the AI-first C++23 build skeleton + its `AGENTS.md` (see below) |

```bash
# from your project root, pulling from a local clone of Hephaestus
mkdir -p .opencode/skills .opencode/agent
cp -R /path/to/Hephaestus/.opencode/skills/* .opencode/skills/
cp -R /path/to/Hephaestus/.opencode/agent/*  .opencode/agent/   # custom agents
cp    /path/to/Hephaestus/opencode.json .                       # default model + instructions
cp    /path/to/Hephaestus/AGENTS.md .                           # optional but recommended
```

```powershell
# Windows PowerShell
New-Item -ItemType Directory -Force .\.opencode\skills, .\.opencode\agent | Out-Null
Copy-Item -Recurse \path\to\Hephaestus\.opencode\skills\* .\.opencode\skills\
Copy-Item -Recurse \path\to\Hephaestus\.opencode\agent\*  .\.opencode\agent\
Copy-Item \path\to\Hephaestus\opencode.json .
Copy-Item \path\to\Hephaestus\AGENTS.md .            # optional but recommended
```

Then run `opencode` in your project, `/models` to confirm the model, and `/agents` to
confirm agents are available. Trim the set to what you need (e.g. drop the `graphics-*`
or `cpp-template-workflow` skills/agents if your project doesn't use them), and tailor
`AGENTS.md` to your repo's conventions. Update the default `model` in `opencode.json` (and
the authoritative tier mapping) to match the providers you have connected.

### Where the C++ template goes

`cpp/` is a **standalone project skeleton** (its own `CMakeLists.txt`, `CMakePresets.json`,
`AGENTS.md`, `.clang-tidy`, `.clang-format`, `src/`, `include/`, `tests/`). Its tooling
expects to run from **the directory that contains `CMakeLists.txt`**. Two ways to reuse it:

- **As the whole project** — copy the **contents** of `cpp/` into your project root, so
  `CMakeLists.txt` and `AGENTS.md` sit at the root. Run `opencode` from the root and the
  C++ command manifest in `AGENTS.md` loads automatically when you work under `cpp/`. If
  you also want the Hephaestus workflow `AGENTS.md`, **merge the two** into one root
  `AGENTS.md` (a folder has only one).
- **As a subproject/library** — keep it in a subfolder (e.g. `cpp/` or `libs/<name>/`).
  Its `AGENTS.md` applies when that folder is your current working directory, so run
  `opencode` from inside that folder for the C++ commands.

### What *not* to do

- ❌ Don't hard-code a model in an agent or `opencode.json` beyond the default — reference
  a **tier** and let the single authoritative mapping in `software-plan-orchestration`
  resolve it.
- ❌ Don't rename `SKILL.md` or rely on the folder name to invoke a skill — identity is the
  front-matter `name:` (the numeric prefixes are just ordering labels).
- ❌ Don't nest the C++ template as `cpp/` and then run build commands from the repo root —
  run them from the directory holding `CMakeLists.txt`, or its presets won't resolve.
- ❌ Don't keep two `AGENTS.md` files in the **same** folder — opencode loads one per
  git-root/cwd; merge instead.
- ⚠️ Skills in `.opencode/skills/` load for **everyone** who runs `opencode` in that repo —
  only commit the ones the project actually needs.
- ⚠️ Installing into `~/.config/opencode/skills/` is **global** (every project). Use the
  in-repo `.opencode/skills/` path if you want them scoped to one project only.

**C. Make them global instead** — see *Option B* under Install & Use to symlink/copy
`.opencode/skills/*` into `~/.config/opencode/skills/` so they apply to every project.

## Agentic assets in this repo

- `opencode.json` (repo root): project config — default model (`zai-coding-plan/glm-5.2`)
  and `AGENTS.md` instruction wiring.
- `AGENTS.md` (repo root): opencode-first workflow conventions and lifecycle routing.
- `.opencode/skills/*/SKILL.md`: executable skill library (auto-loaded as project skills).
- `.opencode/agent/*.md`: custom agents (5 role + 10 V-model + 8 utility), invoked with
  `/agents`.
- `cpp/AGENTS.md`: canonical command manifest for the C++ template subtree.

## C++ build template

The [`cpp/`](cpp/) directory holds a standalone **AI-first C++23 build
template** — a project skeleton whose tooling emits structured, machine-readable
information optimised for AI agents (compile database, Doxygen XML + tagfile,
clang-tidy fix exports, cppcheck XML), while keeping the check set high-signal
and low-friction so it never blocks code generation.

Highlights:

- **Ninja + Clang/GNU** unlocks the full clang-tidy + cppcheck analysis stack;
  any other toolchain (e.g. MSVC) still builds, with analysis cleanly skipped.
- Two verification levels: **`verify`** (fast default, build+test+analysis status)
  and **`verify-full`** (strict checks including format/static-analysis/docs).
- Machine-readable reports land in stable paths under `build/reports/`.

**Optional MCP server** ([`cpp/mcp/`](cpp/mcp)): exposes the C++ tooling to opencode as
structured tools (`cpp_verify`, `cpp_build`, `cpp_docs`, …) returning JSON. It also runs
**standalone on any existing C++ project** — `cpp_cppcheck`, `cpp_format`, `cpp_clang_tidy`
with caller-supplied settings, no CMake layout required. Install with
`pip install -r cpp/mcp/requirements.txt` and set `mcp.cpp.enabled=true` in `opencode.json`
(it ships disabled so a fresh clone stays clean). See
[`cpp/mcp/README.md`](cpp/mcp/README.md).

See [`cpp/README.md`](cpp/README.md) for the overview and
[`cpp/AGENTS.md`](cpp/AGENTS.md) for the canonical C++ command manifest.

## License

[MIT](LICENSE) © 2026 Norbert Nopper.
