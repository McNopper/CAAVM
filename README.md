# 🔱 Hephaestus

### Cyclic Agentic Agile V-Model — *hybrid top-down + bottom-up*

> Build software increment by increment with full V-Model traceability, TDD,
> adversarial verification, and enforced clean-code gates — driven by autonomous
> agents and **configured in one file**. Top-down sets the architecture *intent*;
> a bottom-up **walking skeleton** of real units proves it; they meet in the middle
> and an **adaptation** step lets the running code refine the design. Defaults target
> **C++**; retarget any language by editing config.

> **Why "Hephaestus"?** [Hephaestus](https://en.wikipedia.org/wiki/Hephaestus) is the Greek god of
> craft and the forge — and the one who built *automatons* (Talos, the golden mechanical attendants).
> A fitting patron for a disciplined, agent-driven build process.

```
 0. MVP loop (INPUT.md ⇄ OUTPUT.md, re-run to deepen — maturity tracked PER SLICE)
 1. Requirements ───────────────────────────────► 10. Acceptance test   (run the system; screenshots/E2E)
 2. Software System (topology + executables) ───►  9. System test       (deployables run together)
 3. Architecture (per executable: modules) ─────►  8. Module test        (mocked)     ┐ contracts
 4. Design (per component: interface + units) ──►  7. Component test     (mocked)     ┘ PROVISIONAL
 4b. Slice select (walking skeleton: thinnest end-to-end vertical slice of REAL units)
 5. Implementation (per unit, own branch, TDD) ─►  6. Unit test
        partial scaffold publishes THIS slice's interfaces + skeleton · each node verified on its branch, merged up only when green
        Adaptation: promote provisional→stable, revise what the code disproved, retire stubs · refactor (any phase) · then quality gate
        commit every phase onto main · re-run to deepen each slice (mvp → harden → complete)
```

Each backlog item runs one full **V-pass** — top-down for *intent*, bottom-up for *evidence*, meeting
in the middle (a **walking skeleton** first), with controlled feedback up — and the V repeats every
iteration (the *cyclic agile* part). The architecture is **provisional** until running code validates
it. Hephaestus builds an **MVP first** and is **re-run to deepen**, with maturity tracked **per slice**:
a run advances every slice that is ready, so `INPUT.md` is resolved over several meaningful loops. Every
phase **commits onto `main`** as it finishes (and writes a file-level trace).

## The mental model — how the V is structured

Read the V as a **composition relationship**: each tier is built from *one or more* of the tier
below it, and each tier is **verified by its own test level**.

```
 software system   — the whole product and its TOPOLOGY (e.g. "client-server")
   └─ software(s)  — one or more executables/deployables (e.g. a client.exe AND a server.exe)
       └─ module(s)     — each executable is architected into modules (e.g. the client is
       │                  Layered with three modules; the server has its own pattern)
           └─ component(s)  — each module is built from components (each with a clear interface)
               └─ unit(s)   — each component is built from units (functions/classes)
```

Spelled out as relationships (each "one or more"):

- **a software system** consists of one or more **executables** (the *topology* — e.g. client-server
  yields two executables); it is verified by the **acceptance test** (run the whole system) and the
  **system test** (the executables run together).
- **an executable (a "software")** consists of one or more **modules**; verified by the **module test**.
- **a module** consists of one or more **components**; verified by the **component test** (mocked).
- **a component** consists of one or more **units**; verified by the **unit test**.
- **a unit** belongs to exactly **one** component. (A component may be *reused* by several modules —
  it is built once.)

The left arm decides this top-down (**phases 1→5**) but its contracts are **provisional**; the right
arm verifies bottom-up (**phases 6→10**), each test level paired with the stage that produced its tier.
Each increment first builds a **walking skeleton** — the thinnest end-to-end vertical slice of *real*
units (chosen at phase 4b) — so the architecture is proven by running code before breadth is added.

**Why it parallelizes.** The decomposition is **interface-first**: as soon as a tier's interfaces
exist, the work beneath it is decoupled. So each executable is architected on its own, every **unit** is
implemented on its **own branch** in parallel (developer-style), and every test level fans out per sibling
(per component, per module, per executable). Work is assembled back
**bottom-up** — units → components → modules → executables → system — each node merging into its parent
**only once its test is green**. It is **highly parallel**.

**Forward construction, backward learning, clear gates, targeted repair.** The left arm flows **forward**
to set intent, but its contracts are provisional and there are **no *uncontrolled* backward jumps**. Gaps
surface in the **test phases** (clear gates); what the running code teaches flows back **up** only through
the explicit **Adaptation** step (promote provisional→stable, revise the disproven, retire stubs). When
a gate goes red, Hephaestus repeats the build loop for **only the failing element** (a unit, a component, a
module, a deployable — the rule is the same at every level) plus refactoring, then re-verifies —
bounded by `max_fix_rounds`. **Already-passing siblings are never re-implemented.** If it can't go
green within the budget, the increment fails its gate and re-loops (the coarse safety net).

## How it's used

Hephaestus is a **drop-in kit you ship inside your own project**, not a separate place where your
code lives. Copy these files into your project root, next to your source, and commit them with it:

```
your-project/
├── INPUT.md                       # you write here
├── OUTPUT.md                      # the process writes here
├── config/hephaestus.config.yaml  # defaults + your overrides
├── docs/hephaestus.*.md           # plan + process spec
├── .claude/workflows/hephaestus.js
├── .gitattributes
└── src/ … (your actual code)
```

**This repository (`McNopper/Hephaestus`) is the upstream** that maintains and improves those kit
files. Your project vendors a copy; to get fixes and new features later, pull the updated
`config/`, `docs/`, and `.claude/workflows/` files from here (your `INPUT.md`/`OUTPUT.md` and your
source are untouched). Treat the kit like a versioned dependency you periodically refresh.

## Features

- 🔁 **Cyclic V-Model (5↔5)** — requirements↔acceptance, software-system↔system, architecture↔module, design↔component, implementation↔unit.
- 🥪 **Hybrid top-down + bottom-up** — top-down sets **provisional** architecture (a hypothesis); a bottom-up **walking skeleton** of real units proves it; they meet in the middle. An **Adaptation** step then promotes validated contracts `provisional→stable` and revises what the code disproved.
- 🧱 **Explicit composition hierarchy** — `system → software(executable) → module → component → unit` (each *one or more*; a unit in exactly one component; a component reusable across modules). Every element is **visible** (in `OUTPUT.md` + traces) and **independently tested** at its level.
- ⚡ **Highly parallel, interface-first** — once a slice's interfaces are published (single-writer **partial** Scaffold) the work decouples: each executable is architected on its own, every **unit** is implemented on its **own branch** (developer-style), and every test level fans out per sibling. Assembled back **bottom-up** as a tree of gated merges (unit→component→module→software→system) — which is what lets it scale to **very complex systems**.
- 🔄 **Controlled feedback (no uncontrolled backward jumps)** — the left arm flows forward to set intent; the bottom informs the top **only** through the explicit Adaptation gate. Each test level is a gate; a red gate repeats the build loop for **only the failing element** (same rule at every level — unit, component, module, deployable) + refactor, then re-verifies. Passing siblings are never re-implemented. Bounded by `max_fix_rounds`.
- 🧾 **Assumption-debt ledger** — every stub/driver/provisional interface is logged with an **owner** + **retirement condition**, carried forward and burned down as slices harden (bounds the sandwich-integration squeeze).
- 🪜 **Per-slice MVP maturity ladder** — each validated slice climbs its **own** rungs (`mvp → harden → complete`); a run advances every slice that is ready; **re-run to deepen** until `INPUT.md` is fully resolved. Each rung scales the quality gates.
- 🌳 **Commit-per-phase, gated-merge tree** — every phase persists + commits its content; each node is built on its own branch (worktree) and **merged into its parent only once its test is green** (unit→component→module→software→system→`main`), so `main` only ever holds verified work. A per-phase **trace file** is written even when docs are `minimal`/`off`, for a file-level trail.
- 🤖 **Agentic** — a dedicated agent per stage; *independent* agents verify (adversarial).
- 🧪 **TDD** — red→green→refactor; tests climb `unit → component → module → system → acceptance`.
- 🧹 **Clean code** — SOLID, Ports & Adapters, smell hunting; refactoring is **on-demand inside every phase** (red→green→refactor), with thresholds enforced at the gate.
- 🚦 **Quality gates** — coverage, complexity, zero-warning lint/format, sanitizers, doc coverage, **produced** traceability matrix, no provisional contracts left in a completed slice.
- 🎚️ **Per-phase model routing** — run each phase on a different model tier (`opus`/`sonnet`/`haiku`), e.g. Opus for architecture/design/adaptation/gate, Haiku for mechanical verification.
- 🛠️ **One-file config** — language, version, tools (clang-format, clang-tidy, cmake, GoogleTest, …), toggles and models are all plain data.

## Repository layout

```
INPUT.md                          # YOU write loose ideas/requirements here (the interface in)
OUTPUT.md                         # process writes a short status checklist here (the interface out)
config/hephaestus.config.yaml     # SOURCE OF TRUTH (sensible defaults) — Intake updates this
docs/hephaestus.plan.md           # methodology & rationale
docs/hephaestus.process.md        # portable step-by-step spec (any tool)
.claude/workflows/hephaestus.js   # runnable Claude Code workflow
```

## The interface: `INPUT.md` ⇄ `OUTPUT.md`

You don't have to edit YAML to use Hephaestus — you talk to it through two markdown files with
**strict ownership**:

- **[`INPUT.md`](INPUT.md) — human-only (the process only ever *reads* it; never writes it).**
  Write plain sentences, anytime (even while it runs): the language, design, tools, gates, and
  feature ideas. The **Intake** step reads it and **rewrites the project files to match** —
  high-level choices go into `config/hephaestus.config.yaml`, feature ideas become backlog
  increments. It can be **super minimal**: the config already holds defaults (C++23, CMake+Ninja,
  clang-tidy/clang-format/cppcheck, GoogleTest, hexagonal design…), so you only write what should
  *differ* — even a single sentence is a valid input.
- **[`OUTPUT.md`](OUTPUT.md) — process-only (you never hand-edit it).** Everything the process
  understands, derives, creates, or updates lands here: what it captured from `INPUT.md`, the
  resolved configuration, and after each increment a short status checklist (per-increment status,
  stage reached, gate result, the five test levels, open debt, next action).

So the everyday loop is: _jot in `INPUT.md` → run → read `OUTPUT.md` → run again (to deepen) /
jot more in `INPUT.md`._ The process never edits your `INPUT.md`; you never edit its `OUTPUT.md`.

**Run it several times.** Hephaestus follows an **MVP strategy** with **per-slice** maturity: a slice's
first touch builds its walking skeleton at `mvp`, and a slice that passed its gate advances toward
`harden → complete` on later runs (`config.strategy`) — so one invocation advances **every slice that is
ready** (set `strategy.maturity_scope: whole_backlog` for the legacy one-rung-per-run behavior). Each
later run reads `OUTPUT.md`, deepens ready slices, and re-checks `INPUT.md`, until every idea is resolved
at the top rung. `OUTPUT.md` shows each slice's rung, the provisional-vs-stable contracts, the
assumption-debt log, and a per-idea resolution table. Every phase commits its content onto `main` as it
finishes, so progress is incremental and a run is resumable.

## Quick start

> 💻 **Runs on the engineer's desktop.** Hephaestus is designed to run locally in your terminal/IDE,
> not in CI. The agent edits code, runs the build, and executes the quality gates against your
> **local toolchain** — so the same machine needs the configured tools installed (e.g. for the
> C++ default: a C++23 compiler, CMake + Ninja, clang-format, clang-tidy, cppcheck, Doxyfile/Doxygen)
> plus your agent CLI (Claude Code or GitHub Copilot CLI). CI, if you add it, just re-runs the
> same gate commands from the config.

**Configure.** Easiest: write a sentence or two in [`INPUT.md`](INPUT.md) and let the Intake step
update the config for you. Or edit [`config/hephaestus.config.yaml`](config/hephaestus.config.yaml)
directly (`language.*`, `toolchain.*`, `quality_gates.*`, `toggles.documentation`, `models.*`,
`project.backlog`). Either way it's the same source of truth, so behavior is identical no matter
which agent drives it.

Then drive it with your agent of choice — Claude Code or GitHub Copilot CLI, both running on your
workstation.

### Using with Claude Code

Two ways, sharing the same config:

**A) Automated workflow (multi-agent).** The script `.claude/workflows/hephaestus.js` is registered
as the `hephaestus` workflow (open this repo as your Claude Code project so `.claude/workflows/` is
discovered). The workflow engine can't read files itself, so ask Claude to load the config and pass
it as `args`:

```text
Run the hephaestus workflow using config/hephaestus.config.yaml as args.
```

Claude parses the YAML, hands it in as `args` (merged over built-in defaults), and runs every
increment end-to-end at **each slice's maturity rung**: requirements → software system → architecture →
design (all components, interfaces only — **provisional**) → **slice select** (the walking skeleton) →
**partial scaffold** (publish that slice's interfaces + a glob build skeleton onto the branch) →
implementation (each **unit** on its own branch, in worktrees, merged into its component) → a **bottom-up tree of gated merges** —
each node is verified in isolation on its branch and merged into its parent only once green
(unit→component→module→software→system), the verified system landing on `main` → **adaptation** (promote
provisional→stable, revise what the code disproved, retire stubs, **produce the traceability matrix**) →
quality gate — committing each phase as it finishes and returning a per-increment report + the produced
traceability matrix. The
carry-forward ledger (decisions, debt, assumption-debt) feeds each increment into the next; **re-run the
workflow to deepen each slice** until `INPUT.md` is fully resolved.

> ⚠️ **This burns a LOT of tokens.** It spawns a subagent per unit, per component, per module, per
> deployable, per test level, per fix round — across every increment and every maturity loop — so cost scales
> with the size of your system and can be very large. **Strongly recommended: set a spend/token limit *before*
> you start**
> (e.g. a token-budget directive like `+500k` on your prompt, or your client's usage cap) so a deep run
> can't run away. Only invoke it once you've opted into multi-agent orchestration (say "use a workflow").

**B) Single-agent / manual.** Tell Claude to follow the portable spec instead — useful for one
increment, smaller token budgets, or step-by-step review:

```text
Follow docs/hephaestus.process.md to implement backlog item INC-001 from
config/hephaestus.config.yaml. Do one V-pass and stop at the quality gate.
```

#### Per-phase model routing

The workflow runs each phase on the model tier named in `config.models` (`opus` / `sonnet` /
`haiku`). The shipped default puts the judgment-heavy phases on Opus and the mechanical ones on
cheaper tiers:

```yaml
models:
  default: sonnet
  system: opus           # topology + deployables (executables)
  architecture: opus     # per-deployable pattern, modules, boundaries, ADRs (provisional)
  design: opus           # component interfaces + design patterns (provisional)
  slice: sonnet          # pick the walking-skeleton vertical slice
  adaptation: opus       # promote provisional→stable + revise disproven architecture
  gate: opus             # final Definition-of-Done judgment
  implementation: sonnet # high-volume parallel TDD (each unit on its own branch)
  requirements: sonnet
  verification: haiku     # mechanical: run the unit/component/module/system/acceptance tests
```

Edit any phase to taste; unset phases fall back to `default`, and with no `models` block at all
every phase inherits your session model. (This routing is a Claude Code capability; on the Copilot
CLI / manual path it's a recommendation, since that path uses one session model.)

### Using with GitHub Copilot CLI

The JS workflow is Claude-Code-specific, so with Copilot you drive the model from the
**tool-agnostic process spec** — same config, same gates. Start the agentic Copilot CLI in the repo:

```bash
copilot          # launch the agentic GitHub Copilot CLI in your project
```

Then instruct it:

```text
Act as the Hephaestus agent. Read config/hephaestus.config.yaml (the single source of truth) and
follow docs/hephaestus.process.md. Build project.backlog item INC-001 as one HYBRID V-pass:
requirements → software-system → architecture → design (contracts PROVISIONAL) → slice-select
(walking skeleton) → partial scaffold → TDD implementation → unit/component/module/system/acceptance
verification → adaptation (promote provisional→stable, revise, retire stubs) → refactor (cross-check
the refactoring & design-pattern catalogs in `references`) → quality gate. Honor toggles.documentation,
the per-slice maturity ladder, and the carry_forward rule (incl. the assumption-debt ledger).
```

**Make it automatic** — drop a custom-instructions file so you don't repeat the context each run
(Copilot CLI auto-loads it):

```markdown
<!-- .github/copilot-instructions.md -->
This repo uses Hephaestus. Treat config/hephaestus.config.yaml as the single source of truth for
language, tools, quality gates, reference catalogs, strategy, git, and toggles.documentation. For
any feature work, follow docs/hephaestus.process.md: build each project.backlog item as one HYBRID
V-pass — top-down architecture is PROVISIONAL, build a walking-skeleton vertical slice of real units
first (partial scaffold), verify bottom-up through the five test levels, then run Adaptation to promote
validated contracts provisional→stable and revise what the code disproved (controlled feedback up, no
uncontrolled backward jumps). Run the configured formatter/linters/tests as gates, produce the
traceability matrix, keep the assumption-debt ledger current, keep docs minimal & effective, and carry
decisions + debt forward. Follow the PER-SLICE maturity ladder (strategy.maturity_levels: mvp → harden →
complete) — build each slice's thinnest skeleton first and deepen on re-runs, judging against that rung's
effective gates. Commit each phase onto the working branch as it finishes and write the per-phase trace
file (living_artifacts.phase_trace).
```

> The same file pattern works for **VS Code Copilot** and other Copilot surfaces, so the V-Model
> process is enforced consistently across the team.

### Any other agentic CLI / IDE

Hephaestus is just three plain files — config + plan + process. Feed
[`docs/hephaestus.process.md`](docs/hephaestus.process.md) and
[`config/hephaestus.config.yaml`](config/hephaestus.config.yaml) as context to any capable
coding agent (Cursor, Aider, Gemini CLI, …) and ask it to execute one V-pass per backlog item.

## Retargeting the language

C++ is just the default. To target e.g. Rust, edit the config: `language.name: Rust`,
`clang-format`→`rustfmt`, `clang-tidy`→`clippy`, `GoogleTest`→`cargo test`, `cmake`→`cargo`.
No other file changes.

## Documentation

- **[Plan & methodology](docs/hephaestus.plan.md)** — the what & why, full V-mapping, diagrams.
- **[Process spec](docs/hephaestus.process.md)** — the how, tool-agnostic, with templates.

## License

[MIT](LICENSE) © 2026 Norbert Nopper.
