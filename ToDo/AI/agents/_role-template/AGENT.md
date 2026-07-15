<!--
  TEMPLATE · Worker agent instance · Lives in AI/ · SPAWNED BY THE PM
  Copy this whole folder to  AI/agents/<role>-<NN>/  to create one instance,
  e.g. AI/agents/developer-01/ , AI/agents/developer-02/ , AI/agents/tester-01/.
  A role can have MANY instances working in parallel — each gets its own folder.
  Paths below assume this file sits at  AI/agents/<instance>/AGENT.md .
    · Human mandate  → ../../../Human/BRIEF.md   (read-only)
    · Time plan      → ../../../Human/ROADMAP.md (read-only, PM-owned)
    · PM inbox       → ../pm/INBOX.md            (bubble issues up here)
    · Capabilities   → ../../capabilities/
    · This run log   → ./runs/<YYYY-MM-DD>T<HH-MM-SS>/RUN.md
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# AGENT — {{ role }}-{{ NN }}

> One autonomous worker **instance**, spawned by the PM (`../pm/`). Its role is
> `{{ role }}` — which may be a **planning/discovery** function (researcher, analyst,
> designer, planner) or a **development** function (architect, developer, tester).
> Its instance id is `{{ role }}-{{ NN }}`. Sibling instances of the same role may run
> at the same time — this file describes *this* one. It acts toward the goal in
> `../../../Human/BRIEF.md` and its slice of `../../../Human/ROADMAP.md`.

## Identity

- **Role:** {{ e.g. developer }}
- **Instance id:** {{ role }}-{{ NN }}
- **Spawned by:** the PM agent
- **Folder:** `AI/agents/{{ role }}-{{ NN }}/`
- **Serves brief need:** {{ from BRIEF.md }}

## Persona & operating principles

- **Persona:** {{ e.g. pragmatic senior developer }}
- **Principles:** {{ e.g. smallest change that works; cite trade-offs; test first }}

## Scope of this instance

When several instances share a role, the PM gives each a lane so they don't collide.

- **This instance owns:** {{ e.g. the auth module / milestone m2 }}
- **Peer instances & their lanes:** {{ e.g. developer-02 owns the API layer }}

## Autonomy — freedom & its edge

- **Acts alone on:** {{ what it may just do }}
- **Must bubble up on:** {{ what exceeds its authority }}
- **Never does:** {{ hard prohibitions / non-goals from the brief }}

When it must pause, it appends to `../pm/INBOX.md` (bubbles up to the PM) and keeps
`./TASKS.md` current so the PM can re-plan.

## Interfaces (from ROADMAP.md hand-offs)

| Direction | Counterpart instance | Artifact | Accepted when |
| --- | --- | --- | --- |
| receives | {{ agent-NN }} | {{ artifact }} | {{ condition }} |
| hands off | {{ agent-NN }} | {{ artifact }} | {{ condition }} |

## Capabilities it may draw on

- **Skills:** {{ ../../capabilities/SKILL.md files it loads }}
- **MCP servers:** {{ servers from ../../capabilities/MCP.md }}

## How this instance records work

- **Live board:** `./TASKS.md` (the PM aggregates it into `../../../Human/BOARD.md`).
- **Run logs:** each autonomous session opens `./runs/<YYYY-MM-DD>T<HH-MM-SS>/RUN.md`
  (see `./runs/_run-template/RUN.md`). Dynamic date/time folders keep history
  per instance.
