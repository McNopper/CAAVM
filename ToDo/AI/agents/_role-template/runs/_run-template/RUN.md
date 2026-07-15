<!--
  TEMPLATE · Run log · One per autonomous session · Lives in AI/
  A "run" is one bounded autonomous working session of a single agent instance.
  Create a dynamic folder named by DATE and TIME, then drop this RUN.md inside:
      AI/agents/<role>-<NN>/runs/<YYYY-MM-DD>T<HH-MM-SS>/RUN.md
  Example: AI/agents/developer-01/runs/2026-07-14T10-40-22/RUN.md
  The date/time/instance in the path make every session traceable and ordered.
  Anything the session produces that isn't a project deliverable (scratch notes,
  logs, intermediate output) can sit next to this file in the same run folder.
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# RUN — {{ role }}-{{ NN }} · {{ YYYY-MM-DD }}T{{ HH-MM-SS }}

## Header

- **Instance:** {{ role }}-{{ NN }}
- **Started:** {{ timestamp }}
- **Ended:** {{ timestamp or "in progress" }}
- **Goal of this session:** {{ the one or two tasks pulled from ../../TASKS.md }}

## Actions (append as you go)

- {{ time }} — {{ what was done }}
- {{ time }} — {{ what was done }}

## Result

- **Produced:** {{ deliverable / commit / artifact — link or path }}
- **Tasks moved to Done:** {{ list }}
- **Self-check vs BRIEF:** {{ which success criteria advanced }}

## Bubble-ups raised this run

- {{ ../../../pm/INBOX.md I-{{ id }} — one line }} · or "none"

## Handoff / next

- **Next session should:** {{ what to pick up }}
