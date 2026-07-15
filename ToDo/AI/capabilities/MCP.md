<!--
  TEMPLATE · Capability pool · Pooled (one entry per server)
  Ownership:    Pooled — called on demand by any agent
  Essence:      External tools, data, and resources — reach beyond the repo.
  Note:         MCP servers stay NEUTRAL toward the goal — they are gear, not a mission.
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# MCP SERVERS — capability registry

> The shared registry of **MCP servers**: external tools, data sources, and
> resources that agents can reach. Pooled like skills — any agent may call any
> server it is permitted to. Servers are **goal-neutral**; scope
> (`../../Human/BRIEF.md`) decides which an agent may actually use.

## Registry

| Server | Provides | Kind (tool / data / resource) | Auth / access | Used by roles |
| --- | --- | --- | --- | --- |
| {{ name }} | {{ capability }} | {{ tool }} | {{ token / none }} | {{ roles }} |
| {{ name }} | {{ capability }} | {{ data }} | {{ scope }} | {{ roles }} |

## Per-server detail

### {{ server name }}

- **Endpoint / reference:** {{ where it lives }}
- **Capabilities exposed:** {{ tools / resources }}
- **When to use:** {{ situation }}
- **Constraints:** {{ rate limits, cost, data sensitivity }}
- **Scope check:** allowed by `../../Human/BRIEF.md`? {{ yes / conditions }}

## Guardrails

- Only call servers permitted by the active project's `../../Human/BRIEF.md`.
- Log notable usage to `../../Human/STATUS.md` so the human observer can see it.
- Never expose secrets in `PLAN.md`, `AGENT.md`, `TASKS.md`, or the observe logs.
