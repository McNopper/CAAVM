# Contracts

## About this document
- **Kind:** `doc` / reference (part of `.opencode/docs/`).
- **Read by:** agents and maintainers; **written by:** maintainers.
- **Related:** complements `domains.md`; the ticket/artifact rules are enforced by the `pm` MCP server and the `pm-create-ticket` skill.

The shared, durable agreements every skill and agent honors. Change these centrally and
update all references.

## Ticket contract (pm MCP server)

A ticket is the **hand-off unit**. Its authoritative shape (per project):

| Field | Type | Notes |
|---|---|---|
| `id` | `T-NNN` (or prefix, e.g. `FR-001`) | minted by `pm_create_ticket(id_prefix=)` |
| `title` | string | active verb, states outcome |
| `description` | string | what + why |
| `type` | `story`/`task`/`bug`/`spike` | |
| `status` | enum | `product-backlog` → `sprint-backlog` → `in-progress` → `in-review` → `done` |
| `blocked` + `blocker` | bool + string | **orthogonal** flag, any active state |
| `sprint` | `S-NN` / null | set by `pm_plan_sprint`; null in backlog |
| `story_points` | int | relative size; `pm-estimate-costs` can later feed a `cost` field |
| `priority` | `low`/`medium`/`high`/`critical` | drives self-claim order |
| `role` | enum (extensible) | `architect`/`developer`/`tester`/`pm`/`cpp-engineer`/`graphics-engineer` → who claims it |
| `assignee` | string | set by `pm_claim_ticket` |
| `acceptance_criteria` | string[] | GIVEN/WHEN/THEN; verification must satisfy all |
| `labels` | string[] | free tags |
| `epic` | string | parent ticket id for traceability |
| `artifacts` | `{kind, ref, note, by, ts}[]` | **where the produced output lives** |
| `history` | append-only | state transitions |
| `comments` | append-only | human/agent notes |

### Artifact kinds (the hand-off locator)

| kind | ref | when |
|---|---|---|
| `file` | `src/renderer/swapchain.cpp` | file(s) created/edited |
| `path` | `build/reports/cppcheck.xml` | a directory or report |
| `git` | `abc1234` or `branch: feat/x` | the commit / branch holding the work |
| `url` | `https://…/diff.png` | a remote resource |
| `doc` | `ARCH-003` | another artifact/ticket id |

**Rule:** a worker records its artifact with `pm_add_artifact` *before* moving the ticket
to `in-review`, so the next agent needs no questions.

## Concurrency contract

- **Self-claim:** workers loop `pm_claim_ticket(role=…)`; the call is atomic
  (threading.RLock + cross-process filelock + temp-rename write) so concurrent agents get
  distinct tickets.
- **Reassign:** a returned ticket (`pm_release_ticket` → `sprint-backlog`, `assignee` cleared)
  can be claimed by a *different* agent.
- **Store:** `mcp/base/locking_store.py` — one JSON doc per project, atomic transactions.

## Role → skill/agent dispatch

| role | owns/claims via | verifies via |
|---|---|---|
| `architect` | `software-system`, `software-architecture` | `test-software-system`, `test-software-architecture` |
| `developer` | `software-requirements`/`design`/`implementation` | matching `test-software-*` |
| `tester` | `test-software-*` | (itself) |
| `pm` | `pm-*` skills | — |
| `cpp-engineer` | `cpp-tools` agent | `cpp-tools` agent |
| `graphics-engineer` | `mcp.graphics` (+ `graphics-expert` for `very-high` work) | `graphics-render-comparison` |

## Model-tier contract

Agents/docs reference **tiers**, never hard-coded model IDs (except `graphics-expert`, which
is pinned to the `very-high` model). The authoritative tier→model mapping lives in
`pm-orchestrate-execution`. Tiers: `very-low`, `low` (default executor), `mid`, `high`
(plan/review), `very-high` (run twice & reconcile). Pick the lowest tier that satisfies the
task; escalate, never de-escalate.

## Worker template

A reusable worker-instance template lives at [`worker-template.md`](worker-template.md).
