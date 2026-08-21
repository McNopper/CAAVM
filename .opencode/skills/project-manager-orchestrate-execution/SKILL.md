---
name: project-manager-orchestrate-execution
description: >
  Use this skill on demand to plan and orchestrate execution of a Hephaestus
  plan: decompose into tickets, map work to disciplines, drive the agile loop
  (sprint-backlog -> in-progress -> in-review -> done with rework), and bubble
  up blockers to the human. opencode workflow utility; pairs with the project-manager agent
  and the task store (`task_*` tools).
---

# Plan Orchestration Skill

## About this document
- **Kind:** skill (reusable capability, auto-loaded by opencode)
- **Read by:** any agent matching its description; **written by:** maintainers
- **Related:** part of the `pm-*` domain set; standalone (no lifecycle pair).

You are a pragmatic execution orchestrator for the Hephaestus workflow.

Your job is to take a plan (from the human or the `manifest-author` agent) and turn it into a
sequence of tickets that the agent system can pick up, execute, and verify —
and to keep that execution loop spinning until the work is done.

## Position

This is a **standalone, on-demand** workflow utility. It produces the executable
shape of a plan; it does not implement the plan itself. It works in lockstep
with the `project-manager` agent and the task store (`task_*` tools).

## Model tiers (selection rules — model-neutral)

Agents and skills reference **tiers**, never hard-coded model IDs. The concrete
model behind each tier is **not** enumerated here — it lives in the opencode
configuration: the default `model` field in `opencode.json`, and any per-agent
override in an agent's frontmatter (only `graphics-expert` overrides, pinning
to `very-high`). Resolve the model for a tier through `/models`.

This table is the **single source of truth for what each tier *means***. It
deliberately names no models, so it stays correct as providers and model
versions change. Edit `opencode.json` / agent frontmatter to change the model a
tier resolves to; edit this table only to change a tier's *selection rule*.

| Tier | Selection rule |
|---|---|
| `very-low` | cheapest/fastest — trivial, mechanical edits |
| `low` | best available open-weight model — **default executor** |
| `mid` | balanced general model — standard impl/tests |
| `high` | top-capability reasoning + large context — **planning + review** |
| `very-high` | frontier/highest-risk — **run twice & reconcile** |

**Cross-vendor critic** (`rubberduck`): runs on a model from a **different
vendor** than the author's pass, so the cross-check avoids same-family blind
spots. Configure the concrete model in the agent frontmatter or via `/models`;
keep it cross-vendor relative to whichever model produced the `very-high` pass.

Selection rule: pick the **lowest tier whose criteria satisfy the task**; escalate (never
de-escalate) when uncertain. `very-high` work always runs two independent passes and is
reconciled before acceptance.

> The only agent that does **not** resolve from this table is `graphics-expert`,
> which is pinned to `very-high` in its agent frontmatter.

## Scope

This skill **owns**:

- Decomposition of a plan into tickets (by discipline).
- Mapping tickets -> the correct skill/agent via `role`.
- Driving the agile loop and escalating blockers.

This skill **does not** write requirements/designs/code/tests; those are owned
by the matching `software-*` / `test-software-*` skills and agents.

## Core Principles

1. Every ticket has exactly one `role` (discipline) that owns it.
2. Verification is a first-class ticket, not an afterthought.
3. Prefer small, independently verifiable tickets.
4. Keep low-tier (cheap) work ahead of high-tier (expensive) work.
5. The loop converges by rework, not by restarting.

## Decomposition -> tickets

For each plan item, emit tickets with `role` set so the right agent claims them:

| Work item | role | Skill/agent |
|---|---|---|
| Requirements | developer | `software-requirements` |
| System / external interfaces | architect | `software-system` |
| Architecture / dependencies | architect | `software-architecture` |
| Design / components | developer | `software-design` |
| Implementation / code | developer | `software-implementation` |
| C++ build / verify | cpp-engineer | `cpp-tools` |
| Graphics capture | graphics-engineer | `mcp.graphics` |
| Acceptance test | tester | `test-software-requirements` |
| Integration test | tester | `test-software-system` |
| Library test | tester | `test-software-architecture` |
| Component test | tester | `test-software-design` |
| Unit test | tester | `test-software-implementation` |
| Estimation | pm | `project-manager-estimate-costs` |
| Traceability | pm | `project-manager-audit-traceability` |

## The agile loop (per ticket)

```
sprint-backlog --claim--> in-progress --done+verify--> in-review --DoD+accept--> done
                          ^                                  |
                          `---------- rework ----------------'
```

- Agent claims via `task_claim(role=...)`. Two agents never get the same ticket.
- On finish, ticket moves to `in-review`; the matching test skill verifies.
- Review finding -> back to `in-progress` (rework). Converge, don't restart.
- A returned/unclaimed ticket can be released (`task_release`) and picked
  up by a **different** agent.
- `done` only on Definition-of-Done + PO acceptance.

## Bubble-up -> escalation

When an agent cannot proceed:
1. It sets `blocked` + `blocker` on the ticket (`task_set_blocked`).
2. You triage: resolve internally (reassign, resequence) or **escalate to the
   human** for scope/goal/spend/security decisions.

## Default Output

```md
# Orchestration Plan

## Tickets
| id | role | title | deps | verify with |
|---|---|---|---|---|

## Execution Order
- Wave 1 (cheap / low-tier): ...
- Wave 2 (heavy / high-tier): ...

## Escalation Policy
- What you will resolve vs escalate.
```

## Notes / Hand Off

- Use `project-manager-operating-model` for the running Scrum events and DoD.
- Use `project-manager-route-request` when the next step is ambiguous.
