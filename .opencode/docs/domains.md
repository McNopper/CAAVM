# Domains

## About this document
- **Kind:** `doc` / reference (part of `.opencode/docs/`).
- **Read by:** agents and maintainers; **written by:** maintainers.
- **Related:** complements `contracts.md`; the dispatch map is mirrored in `pm-orchestrate-execution`.

Hephaestus organizes everything by **domain**, encoded in skill/agent **names**, not in
folder trees. opencode discovers every `SKILL.md` under `.opencode/skills/*/` and every
agent under `.opencode/agent/*.md` flat. The domain is the `<domain>-` prefix of the name.

## Naming convention

`<domain>-<descriptor>`, lowercase, dash-separated. A skill's front-matter `name:` MUST
match its folder name (opencode requirement). Name regex: `^[a-z0-9]+(-[a-z0-9]+)*$`.

## Domain prefixes

| Prefix | Domain | Carries | Examples |
|---|---|---|---|
| `software-` | Definition (what / how) | the left side of the work | `software-requirements`, `software-system`, `software-architecture`, `software-design`, `software-implementation` |
| `test-software-` | Verification | the right side, per level | `test-software-implementation`, `-design`, `-architecture`, `-system`, `-requirements` |
| `pm-` | Project management | tickets, sprints, routing, costing, traceability | `pm-operating-model`, `pm-orchestrate-execution`, `pm-route-request`, `pm-audit-traceability`, `pm-estimate-costs`, `pm-create-ticket` |
| `cpp-` | C++ execution utility | methodology for the `cpp-tools` agent | `cpp-tools` |
| `graphics-` | Graphics utility (thin) | methodology; heavy work is `mcp.graphics` | `graphics-render-comparison` |

## Coordination agents (unprefixed)

`orchestrator`, `planner`, `executor`, `reviewer`, `rubberduck`, `pm`, `cpp-tools`,
`graphics-expert`. The first seven are model-neutral (reference a tier). `graphics-expert`
is the exception: pinned to the `very-high` model.

## Why names, not folders

- Skills auto-load by directory; adding a subfolder doesn't change discovery but does
  make cross-references brittle. Flat + domain-in-name keeps everything one glob away.
- The domain prefix doubles as a dispatch hint: a ticket `role` maps to a prefix
  (`developer` → `software-*`, `tester` → `test-software-*`, `cpp-engineer` → `cpp-tools`,
  `graphics-engineer` → `mcp.graphics` + `graphics-expert`, `architect` → `software-system`
  / `software-architecture`, `pm` → `pm-*`).
