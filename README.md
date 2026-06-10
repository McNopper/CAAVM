# 🔱 Hephaestus

> *[Hephaestus](https://en.wikipedia.org/wiki/Hephaestus) — Greek god of the forge, and the one who
> built automatons (Talos, the golden mechanical attendants). A fitting patron for a disciplined,
> agent-driven build process.*

**Cyclic Agentic Agile V-Model** — *hybrid top-down + bottom-up.*

Ten self-contained skill-agent markdown files — one per V-Model stage. Hand any file to any AI
agent (Claude Code, GitHub Copilot, Cursor, Aider, …) and it executes that stage with full
context, built-in defaults, and clear exit criteria. No orchestration framework required.

Each agent file is **fully self-contained**: built-in C++23/CMake/GoogleTest defaults are embedded;
retarget any language by prepending a short config block. Better focused context → sharper
reasoning → more reliable execution.

```
 1. Requirements ───────────────────────────────► 10. Acceptance test   (run the system; E2E evidence)
 2. Software System (topology + executables) ───►  9. System test       (deployables run together)
 3. Architecture (per executable: modules) ─────►  8. Module test       (mocked)     ┐ contracts
 4. Design (per component: interface + units) ──►  7. Component test    (mocked)     ┘ PROVISIONAL
    Slice select → walking skeleton (thinnest end-to-end slice of REAL units)
 5. Implementation (per unit, own branch, TDD) ─►  6. Unit test
        Scaffold publishes THIS slice's interfaces · each node verified on its own branch, merged up only when green
        Adaptation: promote provisional→stable · retire stubs · produce traceability matrix · then quality gate
        commit every phase · re-run to deepen each slice (mvp → harden → complete)
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

## The 10 skill agents

Copy the `agents/` folder into your project. Each file is a **standalone prompt** — no
orchestration framework, no YAML config to edit first.

| File | Stage | Role | Paired with |
|------|-------|------|-------------|
| [`agents/01-requirements.md`](agents/01-requirements.md) | 1 | Requirements Analyst | Stage 10 |
| [`agents/02-software-system.md`](agents/02-software-system.md) | 2 | Systems Architect | Stage 9 |
| [`agents/03-architecture.md`](agents/03-architecture.md) | 3 | Architect | Stage 8 |
| [`agents/04-design.md`](agents/04-design.md) | 4 | Designer | Stage 7 |
| [`agents/05-implementation.md`](agents/05-implementation.md) | 5 | Implementer (TDD) + Slice Planner + Scaffolder | Stage 6 |
| [`agents/06-unit-test.md`](agents/06-unit-test.md) | 6 | Unit Test Verifier (adversarial) | — |
| [`agents/07-component-test.md`](agents/07-component-test.md) | 7 | Component Test Verifier (adversarial) | — |
| [`agents/08-module-test.md`](agents/08-module-test.md) | 8 | Module Test Verifier (adversarial) | — |
| [`agents/09-system-test.md`](agents/09-system-test.md) | 9 | System Test Verifier (adversarial) | — |
| [`agents/10-acceptance-test.md`](agents/10-acceptance-test.md) | 10 | Acceptance Validator + Adaptation + Gate | — |

Stages 1–5 flow forward (**design**); stages 6–10 verify bottom-up (**evidence**).
Stage 10 also runs Adaptation (promote `provisional→stable`, retire stubs) and the Iteration Gate.

---

## Using the agents

### Invoke a single stage

Give the file directly to your agent as context, then describe what to do:

```
Read agents/01-requirements.md and execute it for backlog item:
{ id: "INC-001", title: "Convert CSV file to JSON" }
Maturity level: mvp
```

Run stages in order — each stage's output is the next stage's input.

### Override built-in defaults

Every agent file embeds C++23/CMake/GoogleTest defaults. To retarget a different language
or toolchain, **prepend a short config block** before the file's content:

```
Language: Rust · Build: cargo · Formatter: rustfmt · Linter: clippy
Test unit: cargo test · Test acceptance: cargo test --test integration
Coverage: cargo llvm-cov

Now read agents/01-requirements.md and execute it for ...
```

Any key you supply overrides the matching default inside the agent file.

---

## Using with Claude Code

Open your project in Claude Code. For a single stage:

```text
Read agents/03-architecture.md and execute it.

Requirements: <paste Stage 01 output>
Software system: <paste Stage 02 output>
Maturity level: mvp
```

For a full V-pass, chain the stages in one session:

```text
Execute the full Hephaestus V-pass for backlog item INC-001 "Convert CSV to JSON".
Follow agents/01-requirements.md through agents/10-acceptance-test.md in order,
passing each stage's output as the next stage's input.
Maturity level: mvp
```

**Model routing** — run judgment-heavy stages on a stronger model:

```text
Use opus for agents 01, 02, 03, 04, 10.
Use sonnet for agents 05.
Use haiku for agents 06, 07, 08, 09.
```

> ⚠️ A full V-pass spawns many subagents (one per unit, component, module, deployable, fix round).
> Set a token budget before starting a multi-agent run — e.g. `+500k` tokens in your prompt.

---

## Using with GitHub Copilot

### Single stage (any Copilot surface)

```text
Read agents/03-architecture.md and execute it.

Requirements: <paste Stage 01 output>
Software system: <paste Stage 02 output>
Maturity level: mvp
```

### Full V-pass (GitHub Copilot CLI)

```text
Execute the full Hephaestus V-pass. For each stage 1–10, read the matching file from
agents/ and execute it, passing each stage's output as the next stage's input.
Backlog item: { id: "INC-001", title: "Convert CSV to JSON" }
Maturity level: mvp
```

### Make it automatic with custom instructions

Drop this file so Copilot auto-loads it on every session:

```markdown
<!-- .github/copilot-instructions.md -->
This project uses the Hephaestus V-Model skill agents in agents/.
For any feature work, execute stages 1–10 in order using the matching agent file.
Each file is self-contained — read it, execute it, pass its output to the next stage.
Left arm (1–5): design and build. Right arm (6–10): verify adversarially, bottom-up.
Stage 10 also runs Adaptation (promote provisional→stable, retire stubs, produce the
traceability matrix) and the Iteration Gate (Definition of Done).
Commit after every stage. Re-run from Stage 01 to deepen the slice to the next maturity
rung (mvp → harden → complete) on subsequent runs.
```

This works for **VS Code Copilot**, **GitHub Copilot CLI**, and any other Copilot surface
that reads `.github/copilot-instructions.md`.

---

## Any other agent

The files are plain markdown — hand any file to Cursor, Aider, Gemini CLI, or any capable
coding agent and ask it to execute that stage. No framework required.

---

## License

[MIT](LICENSE) © 2026 Norbert Nopper.
