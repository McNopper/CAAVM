# 🔱 Hephaestus

> *[Hephaestus](https://en.wikipedia.org/wiki/Hephaestus) — Greek god of the forge, and the one who
> built automatons (Talos, the golden mechanical attendants). A fitting patron for a disciplined,
> agent-driven build process.*

**Cyclic Agentic Agile V-Model** — *hybrid top-down + bottom-up.*

Ten self-contained **skills** — one per V-Model stage. Each is an [Agent Skill](https://code.claude.com/docs/en/skills):
a `SKILL.md` file with `name`/`description` frontmatter that Claude Code, GitHub Copilot, and
other agents load when the task matches. Each skill executes exactly one stage with full
context, built-in defaults, and clear exit criteria. No orchestration framework required.

Each skill is **fully self-contained**: built-in C++23/CMake/GoogleTest defaults are embedded;
retarget any language by prepending a short config block. Better focused context → sharper
reasoning → more reliable execution.

```
 1. Requirements ───────────────────────────────► 10. Acceptance test   (run the system; E2E evidence)
 2. Software System (topology + executables) ───►  9. System test       (deployables run together)
 3. Architecture (per executable: modules) ─────►  8. Module test       (mocked)     ┐ contracts
 4. Design (per component: interface + units) ──►  7. Component test    (mocked)     ┘ PROVISIONAL
    Slice select → walking skeleton (thinnest end-to-end slice of REAL units)
 5. Implementation (per unit, TDD) ──────────────►  6. Unit test
        Scaffold publishes THIS slice's interfaces · each tier verified in isolation, bottom-up
        Adaptation: promote provisional→stable · retire stubs · produce traceability matrix · then quality gate
        re-run to deepen each slice (mvp → harden → complete)
```

---

## The V as a composition hierarchy

Each tier is built from *one or more* of the tier below it and verified by its own test level:

```
 software system  — the whole product and its topology (e.g. "client-server")
   └─ executable(s) — one or more deployables (e.g. client.exe + server.exe)
       └─ module(s)    — each executable is decomposed into modules
           └─ component(s) — each module is built from components (each with a clear interface)
               └─ unit(s)  — each component is built from units (functions/classes)
```

- A **unit** belongs to exactly one component.
- A **component** may serve more than one module — it is built once, reused.
- All contracts start **provisional** (a hypothesis); running code promotes them to **stable**
  at the Adaptation step.
- The **walking skeleton** (Stage 05) is the thinnest end-to-end slice of *real* units that
  proves the riskiest architectural assumptions before any breadth is added.

---

## The 10 skills

Each stage is a skill folder containing a `SKILL.md`:

```
skills/
  01-requirements/SKILL.md
  02-software-system/SKILL.md
  …
  10-acceptance-test/SKILL.md
```

| Skill | Stage | Role | Paired with |
|-------|-------|------|-------------|
| [`skills/01-requirements`](skills/01-requirements/SKILL.md) | 1 | Requirements Analyst | Stage 10 |
| [`skills/02-software-system`](skills/02-software-system/SKILL.md) | 2 | Systems Architect | Stage 9 |
| [`skills/03-architecture`](skills/03-architecture/SKILL.md) | 3 | Architect | Stage 8 |
| [`skills/04-design`](skills/04-design/SKILL.md) | 4 | Designer | Stage 7 |
| [`skills/05-implementation`](skills/05-implementation/SKILL.md) | 5 | Implementer (TDD) + Slice Planner + Scaffolder | Stage 6 |
| [`skills/06-unit-test`](skills/06-unit-test/SKILL.md) | 6 | Unit Test Verifier (adversarial) | — |
| [`skills/07-component-test`](skills/07-component-test/SKILL.md) | 7 | Component Test Verifier (adversarial) | — |
| [`skills/08-module-test`](skills/08-module-test/SKILL.md) | 8 | Module Test Verifier (adversarial) | — |
| [`skills/09-system-test`](skills/09-system-test/SKILL.md) | 9 | System Test Verifier (adversarial) | — |
| [`skills/10-acceptance-test`](skills/10-acceptance-test/SKILL.md) | 10 | Acceptance Validator + Adaptation + Gate | — |

Stages 1–5 flow forward (**design**); stages 6–10 verify bottom-up (**evidence**).
Stage 10 also runs Adaptation (promote `provisional→stable`, retire stubs) and the Iteration Gate.

### Scope: phase execution only

Each skill **executes its own phase and nothing else**. It reads its inputs, does the work,
writes its artifacts (source, tests, trace file), and returns a structured result.

The skills deliberately **do not** touch version control or orchestration: no `git` commits, no
branches, no worktrees, no merges, no "merge to main". Sequencing the stages, persisting each
phase, and assembling the bottom-up tree of verified work are a **separate orchestration layer**
that may be added later. Today you drive the sequence yourself (or let one agent chain the
stages in a single session), and commit when you choose.

---

## Using the skills

> **What you have today:** the 10 skills, nothing more. There is no orchestrator yet — you
> run the stages **yourself, one at a time, in order**, pasting each stage's output into the
> next. (An orchestration layer that chains them automatically may come later.)

Each skill is an [Agent Skill](https://code.claude.com/docs/en/skills): a `SKILL.md` file your
AI tool loads when the task matches its `description`. Below are the **standard** ways to use
them in each tool.

### Claude Code

Claude Code discovers skills under `.claude/skills/`. ([Claude Code → Skills docs](https://code.claude.com/docs/en/skills))

1. Copy the stage folders into your project's `.claude/skills/` (just this project), or into
   `~/.claude/skills/` (every project):
   ```
   cp -r skills/* .claude/skills/
   ```
2. Start Claude Code — it discovers the skills automatically.
3. Ask in plain language, naming the stage and giving it inputs:
   ```
   Use the 01-requirements skill for: convert a CSV file to JSON.
   Maturity level: mvp
   ```
4. When it finishes, hand its output to the next stage:
   ```
   Use the 02-software-system skill with that requirements output as input.
   ```

Repeat 01 → 10. Each skill does exactly one stage, then stops.

### GitHub Copilot

Copilot's standard mechanisms are **prompt files** (invoke on demand) and **repository custom
instructions** (applied automatically).

**Option A — prompt files** (run a stage with a slash command).
([VS Code → Prompt files docs](https://code.visualstudio.com/docs/agent-customization/prompt-files))

1. Copy each skill into `.github/prompts/` as a `.prompt.md` file, e.g.
   `.github/prompts/01-requirements.prompt.md`.
2. In Copilot Chat, type `/01-requirements` and add your inputs.

**Option B — repository custom instructions** (point Copilot at the skills once).
([GitHub → Repository custom instructions docs](https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/add-custom-instructions/add-repository-instructions))

Create `.github/copilot-instructions.md`:

```markdown
This project uses the Hephaestus V-Model skills in skills/.
To build a feature, work through skills 01-requirements … 10-acceptance-test one at a time,
in order, passing each stage's output to the next. Each SKILL.md is self-contained — read it,
do that one stage, then stop.
The skills only execute phase logic; they do not commit or merge — you handle version control.
```

**Simplest start (any Copilot surface):** open a skill and tell Copilot to follow it:

```text
Follow skills/03-architecture/SKILL.md for:
Requirements: <paste Stage 01 output>
Software system: <paste Stage 02 output>
Maturity level: mvp
```

### Any other agent (Cursor, Aider, Gemini CLI, …)

Each `skills/<stage>/SKILL.md` is plain markdown. Open the file for the stage you want and ask
your agent to follow it, giving it the previous stage's output as input. No framework required.

### Retarget the language

Every skill ships with C++23/CMake/GoogleTest defaults. To use another stack, paste a short
config block before invoking the skill — it overrides the matching defaults:

```
Language: Rust · Build: cargo · Formatter: rustfmt · Linter: clippy
Test unit: cargo test · Test acceptance: cargo test --test integration
Coverage: cargo llvm-cov

Now use the 01-requirements skill for ...
```

---

## License

[MIT](LICENSE) © 2026 Norbert Nopper.
