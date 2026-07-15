<!--
  TEMPLATE · Capability pool · Pooled (one file per skill)
  Ownership:    Pooled — loaded on demand by whichever agent needs it
  Essence:      A reusable method, loaded on demand by any agent.
  Note:         A skill stays NEUTRAL toward the goal — it is a method, not a mission.
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# SKILL — {{ skill name }}

> A **reusable method** in the shared pool. Any agent (see `agents/`) may load this
> on demand. It is deliberately **goal-neutral**: it describes *how* to do a thing,
> not *why* — the why stays in `../../Human/BRIEF.md`.

## Identity

- **Skill:** {{ short name }}
- **Purpose:** {{ what capability it adds }}
- **Owner / maintainer:** {{ who keeps it current }}
- **Last updated:** {{ date }}

## When to use

- **Use when:** {{ trigger / situation }}
- **Do not use when:** {{ anti-pattern }}

## Inputs → Outputs

| Inputs required | Produces |
| --- | --- |
| {{ input }} | {{ output }} |

## Procedure

Step-by-step, reproducible method.

1. {{ step }}
2. {{ step }}
3. {{ step }}

## Guardrails

- {{ safety / quality constraint }}
- Respect the active project's `../../Human/BRIEF.md` scope & non-goals.

## Used by

- {{ which roles/agents typically load this skill }}
