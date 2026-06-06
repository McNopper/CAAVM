# CAAVM — Cyclic Agentic Agile V-Model

> Build software increment by increment with full V-Model traceability, TDD,
> adversarial verification, and enforced clean-code gates — driven by autonomous
> agents and **configured in one file**. Defaults target **C++**; retarget any
> language by editing config.

```
Requirements ───────────────► Acceptance / SW-Integration test
   Architecture ────────────► Module / Integration test
      Design ───────────────► Component test
         Implementation ────► Unit test
                  └► refactor ► quality gate ► next increment
```

Each backlog item runs one full **V-pass** (design top-down, verify bottom-up),
and the V repeats every iteration — that's the *cyclic agile* part.

## Features

- 🔁 **Cyclic V-Model** — requirements↔acceptance, architecture↔integration, design↔component, implementation↔unit.
- 🤖 **Agentic** — a dedicated agent per stage; *independent* agents verify (adversarial).
- 🧪 **TDD** — red→green→refactor; tests climb `unit → component → module → integration → acceptance`.
- 🧹 **Clean code** — SOLID, Ports & Adapters, smell hunting, bounded refactor pass every increment.
- 🚦 **Quality gates** — coverage, complexity, zero-warning lint/format, sanitizers, doc coverage, traceability.
- 🎚️ **Per-phase model routing** — run each phase on a different model tier (`opus`/`sonnet`/`haiku`), e.g. Opus for architecture/design/gate, Haiku for mechanical verification.
- 🛠️ **One-file config** — language, version, tools (clang-format, clang-tidy, cmake, GoogleTest, …), toggles and models are all plain data.

## Repository layout

```
config/caav-model.config.yaml     # SOURCE OF TRUTH — edit this
docs/caav-model.plan.md           # methodology & rationale
docs/caav-model.process.md        # portable step-by-step spec (any tool)
.claude/workflows/caav-model.js   # runnable Claude Code workflow
```

## Quick start

> 💻 **Runs on the engineer's desktop.** CAAVM is designed to run locally in your terminal/IDE,
> not in CI. The agent edits code, runs the build, and executes the quality gates against your
> **local toolchain** — so the same machine needs the configured tools installed (e.g. for the
> C++ default: a C++23 compiler, CMake + Ninja, clang-format, clang-tidy, cppcheck, Doxyfile/Doxygen)
> plus your agent CLI (Claude Code or GitHub Copilot CLI). CI, if you add it, just re-runs the
> same gate commands from the config.

**Configure once.** Open [`config/caav-model.config.yaml`](config/caav-model.config.yaml) and set
`language.*`, `toolchain.*`, `quality_gates.*`, `toggles.documentation`, and your
`project.backlog` (each item = one V-pass). Every tool below reads this same file, so behavior
is identical no matter which agent drives it.

Then drive it with your agent of choice — Claude Code or GitHub Copilot CLI, both running on your
workstation.

### Using with Claude Code

Two ways, sharing the same config:

**A) Automated workflow (multi-agent).** The script `.claude/workflows/caav-model.js` is registered
as the `caav-model` workflow (open this repo as your Claude Code project so `.claude/workflows/` is
discovered). The workflow engine can't read files itself, so ask Claude to load the config and pass
it as `args`:

```text
Run the caav-model workflow using config/caav-model.config.yaml as args.
```

Claude parses the YAML, hands it in as `args` (merged over built-in defaults), and runs every
increment end-to-end: requirements → architecture → design → TDD implementation → 5-level
verification (independent agents) → bounded refactor pass → quality gate, returning a per-increment
report + traceability matrix. The carry-forward ledger feeds each increment into the next.

> ⚠️ This spawns many subagents and can use significant tokens — run it when you've opted into
> multi-agent orchestration (say "use a workflow").

**B) Single-agent / manual.** Tell Claude to follow the portable spec instead — useful for one
increment, smaller token budgets, or step-by-step review:

```text
Follow docs/caav-model.process.md to implement backlog item INC-001 from
config/caav-model.config.yaml. Do one V-pass and stop at the quality gate.
```

#### Per-phase model routing

The workflow runs each phase on the model tier named in `config.models` (`opus` / `sonnet` /
`haiku`). The shipped default puts the judgment-heavy phases on Opus and the mechanical ones on
cheaper tiers:

```yaml
models:
  default: sonnet
  architecture: opus     # pattern choice, boundaries, ADRs
  design: opus           # interfaces + design patterns
  gate: opus             # final Definition-of-Done judgment
  implementation: sonnet # high-volume parallel TDD
  requirements: sonnet
  refactor: sonnet
  verification: haiku    # mechanical: run tests, report pass/fail
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
Act as the CAAVM agent. Read config/caav-model.config.yaml (the single source of truth) and
follow docs/caav-model.process.md. Build project.backlog item INC-001 as one V-pass:
requirements → architecture → design → TDD implementation → unit/component/module/integration/
acceptance verification → refactor (cross-check the refactoring & design-pattern catalogs in
`references`) → quality gate. Honor toggles.documentation and the carry_forward rule.
```

**Make it automatic** — drop a custom-instructions file so you don't repeat the context each run
(Copilot CLI auto-loads it):

```markdown
<!-- .github/copilot-instructions.md -->
This repo uses CAAVM. Treat config/caav-model.config.yaml as the single source of truth for
language, tools, quality gates, reference catalogs, and toggles.documentation. For any feature
work, follow docs/caav-model.process.md: build each project.backlog item as one V-pass with TDD
and the five test levels, run the configured formatter/linters/tests as gates, keep docs minimal
& effective, and carry decisions + debt forward between increments.
```

> The same file pattern works for **VS Code Copilot** and other Copilot surfaces, so the V-Model
> process is enforced consistently across the team.

### Any other agentic CLI / IDE

CAAVM is just three plain files — config + plan + process. Feed
[`docs/caav-model.process.md`](docs/caav-model.process.md) and
[`config/caav-model.config.yaml`](config/caav-model.config.yaml) as context to any capable
coding agent (Cursor, Aider, Gemini CLI, …) and ask it to execute one V-pass per backlog item.

## Retargeting the language

C++ is just the default. To target e.g. Rust, edit the config: `language.name: Rust`,
`clang-format`→`rustfmt`, `clang-tidy`→`clippy`, `GoogleTest`→`cargo test`, `cmake`→`cargo`.
No other file changes.

## Documentation

- **[Plan & methodology](docs/caav-model.plan.md)** — the what & why, full V-mapping, diagrams.
- **[Process spec](docs/caav-model.process.md)** — the how, tool-agnostic, with templates.

## License

[MIT](LICENSE) © 2026 Norbert Nopper.
