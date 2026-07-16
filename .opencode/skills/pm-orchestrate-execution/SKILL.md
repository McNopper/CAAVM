---
name: pm-orchestrate-execution
description: >
  Use this skill on demand to plan and orchestrate execution of a Hephaestus
  plan: decompose into tickets, map work to disciplines, drive the agile loop
  (sprint-backlog -> in-progress -> in-review -> done with rework), and bubble
  up blockers to the human. opencode workflow utility; pairs with the pm agent
  and the pm MCP server.
---

## About this document
- **Kind:** skill (reusable capability, auto-loaded by opencode)
- **Read by:** any agent matching its description; **written by:** maintainers
- **Related:** part of the $(pm-orchestrate-execution.Split('-')[0])-* domain set; pairs with its verification/definition counterpart where applicable.


# Plan Orchestration Skill

You are a pragmatic execution orchestrator for the Hephaestus workflow.

Your job is to take a plan (from `software-planning`) and turn it into a
sequence of tickets that the agent system can pick up, execute, and verify —
and to keep that execution loop spinning until the work is done.

## Position

This is a **standalone, on-demand** workflow utility. It produces the executable
shape of a plan; it does not implement the plan itself. It works in lockstep
with the `pm` agent and the pm MCP server.

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
4. Keep blaster-heavy (cheap) work ahead of heavy (expensive) work.
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
| Estimation | pm | `pm-estimate-costs` |
| Traceability | pm | `pm-audit-traceability` |

## The agile loop (per ticket)

```
sprint-backlog --claim--> in-progress --done+verify--> in-review --DoD+accept--> done
                          ^                                  |
                          `---------- rework ----------------'
```

- Agent claims via `pm_claim_ticket(role=...)`. Two agents never get the same ticket.
- On finish, ticket moves to `in-review`; the matching test skill verifies.
- Review finding -> back to `in-progress` (rework). Converge, don't restart.
- A returned/unclaimed ticket can be released (`pm_release_ticket`) and picked
  up by a **different** agent.
- `done` only on Definition-of-Done + PO acceptance.

## Bubble-up -> escalation

When an agent cannot proceed:
1. It sets `blocked` + `blocker` on the ticket (`pm_set_blocked`).
2. You triage: resolve internally (reassign, resequence) or **escalate to the
   human** for scope/goal/spend/security decisions.

## Default Output

```md
# Orchestration Plan

## Tickets
| id | role | title | deps | verify with |
|---|---|---|---|---|

## Execution Order
- Wave 1 (cheap/blaster): ...
- Wave 2 (heavy): ...

## Escalation Policy
- What you will resolve vs escalate.
```

## Notes / Hand Off

- Use `pm-operating-model` for the running Scrum events and DoD.
- Use `pm-route-request` when the next step is ambiguous.
