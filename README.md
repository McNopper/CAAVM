# 🔱 Hephaestus

> *[Hephaestus](https://en.wikipedia.org/wiki/Hephaestus) — Greek god of the forge, and the one who
> built automatons (Talos, the golden mechanical attendants). A fitting patron for a disciplined,
> agent-driven build process.*

## Skills

Hephaestus provides ten **V-model lifecycle** skills arranged as a software lifecycle. The
left side defines the software; the right side verifies it. Each definition skill is
paired with exactly one verification skill, and every skill is scoped so it does not
overlap its neighbours. The skills are deliberately kept at a lean **hobby-project**
level — minimal but useful; a heavier production variant could live as a separate set.

| Definition (left)                  | ↔ | Verification (right)                   |
|------------------------------------|---|----------------------------------------|
| `01-software-requirements`         | ↔ | `10-software-acceptance-test`          |
| `02-software-system`               | ↔ | `09-software-integration-test`         |
| `03-software-architecture`         | ↔ | `08-software-library-test`              |
| `04-software-design`               | ↔ | `07-software-component-test`           |
| `05-software-implementation`       | ↔ | `06-software-unit-test`                |

Each skill lives in `.github/skills/<name>/SKILL.md` and follows the same lean template:
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

## Copilot workflow (recommended)

1. **Start with intent, not implementation:** "Use `software-requirements` to capture goals and acceptance criteria."
2. **Move down the left side:** requirements → system → architecture → design → implementation.
3. **Verify across the right side:** unit/component/library/integration/acceptance tests.
4. **Use explicit hand-offs:** ask Copilot to "handoff to `<next-skill>`" when outputs are ready.
5. **Use CLI controls:** `/skills` to confirm installed skills, `/env` to confirm they are loaded.
6. **For C++ work in this repo:** invoke `cpp-template-workflow` so tasks use `cpp/` and its canonical commands.

### Copilot CLI features worth using here

| Feature | Why it matters in this repo |
|---|---|
| `/plan` | Build a structured implementation plan before multi-file edits. |
| `/autopilot` + `/fleet` | Parallelize doc/skill updates and larger refactors. |
| `/tasks` | Track delegated/background task progress. |
| `/agent` + `/model` + `/subagents` | Choose appropriate specialist agents and model depth for lifecycle stage. |
| `/skills` + `/env` | Verify skill installation/loading and active instruction context. |
| `/review` + `/security-review` | Run focused code/security review passes before merging. |
| `/diff` + `/pr` + `/delegate` | Inspect changes, manage PR flow, and optionally delegate cloud PR creation. |
| `/research` | Gather external references when terminology/process guidance must be defensible. |
| `/memory` | Keep durable conventions/preferences available across sessions. |
| `/instructions` + `/init` | Manage repo instruction sources (`AGENTS.md`, copilot instructions files). |

### Prompting pattern

Use concrete phrasing so skill matching is deterministic:

- "Use the `software-system` skill to define major parts, storage, and external interfaces."
- "Use the `software-library-test` skill to verify architecture boundaries and dependency rules."
- "Use the `software-vmodel-navigation` skill to route this request to the correct lifecycle step."

> Invoke skills by their front-matter `name` (e.g. `software-system`), not the
> numbered folder label (`02-software-system`). The numeric prefixes above are
> only ordering labels for the directories.

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
| `software-plan-orchestration` | Review/rubberduck a plan, order tasks by dependency, tag them with a model tier, and drive automatic execution via Copilot. |
| `cpp-template-workflow` | Automatically route C++ implementation/verification work through the `cpp/` template and canonical command targets. |

#### Model-tier tags (used by `software-plan-orchestration`)

Each ordered task is tagged with one tier, which selects the agent model automatically:

| Tier tag | Agent model | Use for |
|---|---|---|
| `low` | Claude Haiku 4.5 | trivial/mechanical edits, renames, doc tweaks |
| `mid` | Claude Sonnet | standard implementation and tests |
| `high` | Claude Opus (1M context) | complex, cross-cutting, high-context work |
| `very-high` | Claude Fable (1M context) | hardest reasoning / highest-risk tasks (**run twice**, reconcile) |

The rubberduck review pass uses a **different-vendor** model at a comparable tier (e.g. a
GPT-5.x or Gemini 3.x Pro model) so the critic is not the same family as the author.

## Install & Use (GitHub Copilot CLI)

The skills follow the portable [agent skills](https://docs.github.com/copilot/how-tos/use-copilot-agents/use-copilot-cli)
format (`SKILL.md` with `name` + `description` YAML front matter). The Copilot CLI
loads skills from these locations:

- **Project (this repo):** `.github/skills/`, `.agents/skills/`, or `.claude/skills/`
- **Personal (global):** `~/.copilot/skills/` or `~/.agents/skills/`
- **Custom:** any directory added with `/skills add <dir>`

### Option A — use them in this repo (zero install)

Because the skills live in **`.github/skills/`**, they are **auto-loaded as project
skills** whenever you run `copilot` from this repository. Just start a session here
and run `/skills list` to confirm. This is the fastest way to use or develop them.

### Option B — install globally for every project

Copy (or symlink) the skill folders into your personal skills directory:

```bash
# macOS/Linux — symlink so the skills stay in sync with this repo
ln -s "$(pwd)"/.github/skills/* ~/.copilot/skills/
```

```powershell
# Windows PowerShell — copy the skill folders
Copy-Item -Recurse .\.github\skills\* "$env:USERPROFILE\.copilot\skills\"
```

- macOS/Linux personal dir: `~/.copilot/skills/`
- Windows personal dir: `%USERPROFILE%\.copilot\skills\`
- Recommended for global installs: include only the generic lifecycle/utility skills.
  Keep `cpp-template-workflow` project-scoped unless the target repo also contains
  this same `cpp/` template layout.

### Managing skills in a session

- `/skills list` — list all loaded skills.
- `/skills info <name>` — show details of one skill.
- `/skills add [--project] <file|url|directory>` — add a skill (use `--project`
  to write it into the repo's `.github/skills`).
- `/skills reload` — reload after editing a `SKILL.md`.
- `/env` — verify which skills/instructions are loaded for the current session.

**Triggering:** skills are invoked automatically by matching your request against each
skill's `description`. You can also name skills explicitly (recommended for precision),
using the front-matter `name` (e.g. `software-system`), not the folder label.

## Reuse in your own project (template repo)

Hephaestus is a **template repo** — you can adopt the whole thing or cherry-pick parts.

**A. Start a new repo from it** — if this repo is marked as a GitHub template, click
**"Use this template"** (otherwise fork or clone).
Your new repo already has `.github/skills/` and `AGENTS.md`, so Copilot CLI auto-loads
them the moment you run `copilot` in it.

**B. Add it to an existing project** — copy the pieces you want into your repo:

| Copy this | Into your repo at | Gives you |
|---|---|---|
| `.github/skills/*` | `.github/skills/` | the V-model + on-demand skills (auto-loaded as project skills) |
| `AGENTS.md` | repo root | repo-level Copilot workflow/routing conventions (auto-loaded from git root & cwd) |
| `cpp/` contents | your C++ project's root **or** a subdir | the AI-first C++23 build skeleton + its `AGENTS.md` (see below) |

```bash
# from your project root, pulling from a local clone of Hephaestus
mkdir -p .github/skills
cp -R /path/to/Hephaestus/.github/skills/* .github/skills/
cp    /path/to/Hephaestus/AGENTS.md .          # optional but recommended
```

```powershell
# Windows PowerShell
New-Item -ItemType Directory -Force .\.github\skills | Out-Null
Copy-Item -Recurse \path\to\Hephaestus\.github\skills\* .\.github\skills\
Copy-Item \path\to\Hephaestus\AGENTS.md .            # optional but recommended
```

Then run `copilot` in your project and `/skills list` to confirm they loaded. Trim the
set to what you need (e.g. drop the `graphics-*` or `cpp-template-workflow` skills if
your project doesn't use them), and tailor `AGENTS.md` to your repo's conventions.

### Where the C++ template goes

`cpp/` is a **standalone project skeleton** (its own `CMakeLists.txt`, `CMakePresets.json`,
`AGENTS.md`, `.clang-tidy`, `.clang-format`, `src/`, `include/`, `tests/`). Its tooling
expects to run from **the directory that contains `CMakeLists.txt`**. Two ways to reuse it:

- **As the whole project** — copy the **contents** of `cpp/` into your project root, so
  `CMakeLists.txt` and `AGENTS.md` sit at the root. Run `copilot` from the root and the
  C++ command manifest in `AGENTS.md` loads automatically. If you also want the Hephaestus
  workflow `AGENTS.md`, **merge the two** into one root `AGENTS.md` (a folder has only one).
- **As a subproject/library** — keep it in a subfolder (e.g. `cpp/` or `libs/<name>/`).
  Its `AGENTS.md` then only auto-applies when that folder is the git root **or** your
  current working directory, so run `copilot` from inside that folder for the C++ commands.

### What *not* to do

- ❌ Don't put skills in a top-level `skills/` (or any other) folder — only `.github/skills/`,
  `.agents/skills/`, `.claude/skills/` (project) and `~/.copilot/skills/` (personal) are loaded.
- ❌ Don't rename `SKILL.md` or rely on the folder name to invoke a skill — identity is the
  front-matter `name:` (the numeric prefixes are just ordering labels).
- ❌ Don't nest the C++ template as `cpp/` and then run build commands from the repo root —
  run them from the directory holding `CMakeLists.txt`, or its presets won't resolve.
- ❌ Don't keep two `AGENTS.md` files in the **same** folder — Copilot loads one per
  git-root/cwd; merge instead.
- ⚠️ Skills in `.github/skills/` load for **everyone** who runs `copilot` in that repo —
  only commit the ones the project actually needs.
- ⚠️ Installing into `~/.copilot/skills/` is **global** (every project). Use the in-repo
  `.github/skills/` path if you want them scoped to one project only.

**C. Make them global instead** — see *Option B* under Install & Use to symlink/copy
`.github/skills/*` into `~/.copilot/skills/` so they apply to every project.

## Agentic assets in this repo

- `AGENTS.md` (repo root): Copilot-first workflow conventions and lifecycle routing guidance.
- `.github/skills/*/SKILL.md`: executable skill library (auto-loaded as project skills in this repo).
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

See [`cpp/README.md`](cpp/README.md) for the overview and
[`cpp/AGENTS.md`](cpp/AGENTS.md) for the canonical C++ command manifest.

## License

[MIT](LICENSE) © 2026 Norbert Nopper.
