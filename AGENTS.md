# AGENTS.md — Hephaestus Copilot workflow

Repository-level conventions for agentic work in this repository.

## Lifecycle routing (V-model)

Use these skills as the default path:

```text
Software Requirements    (01) ↔ Acceptance Test       (10)
Software System          (02) ↔ Integration Test      (09)
Software Architecture    (03) ↔ Library Test          (08)
Software Design          (04) ↔ Component Test        (07)
Software Implementation  (05) ↔ Unit Test             (06)
```

If a request is ambiguous, route with `software-vmodel-navigation` first.

## Canonical composition hierarchy

The dividing line is **reuse scope** (static-vs-shared linkage is a build decision):

- **Unit** → smallest implementation element with a clear interface.
- **Component** → composed of units; **internal** to this software (linked in).
- **Library** → composed of components; **independently deployable and reusable
  outside this software**; exposes a clear interface and dependency rules.
- **Software system** → composed of libraries plus external/system interfaces.
- **Package/folder** (and language *modules*) → organization only; not a lifecycle level.

## C++ template routing

For C++ tasks in this repository, default to the `cpp/` template workflow:

1. Use `cpp-template-workflow`.
2. Run commands from `cpp/`.
3. Use canonical targets from `cpp/AGENTS.md` (`verify`, `verify-full`, `format`, `tidy`, `cppcheck*`, `docs`).
4. Treat "analysis skipped" on unsupported toolchains as degraded signal, not failure.

## Traceability expectations

- Keep explicit links between each left-side skill artifact and its right-side verification artifact.
- Use `software-traceability-audit` when trace links are missing or unclear.

## Copilot CLI feature usage (recommended)

- Use `/plan` for multi-file or multi-phase changes before implementation.
- Use `/autopilot` and `/fleet` for parallelizable work across many skill/docs files.
- Use `/tasks` to monitor delegated/background work.
- Use `/skills` and `/env` to validate that repository skills and instructions are loaded.
- Use `/review` and `/security-review` before merge when code changes are involved.
- Use `/research` when terminology or process decisions require external references.
