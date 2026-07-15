<!--
  TEMPLATE · Worker agent instance · Lives in AI/ · The live board
  Owner:     This instance (updated continuously as it works)
  Path:      AI/agents/<role>-<NN>/TASKS.md
  The PM reads this each loop and aggregates it into ../../../Human/BOARD.md (Kanban).
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# TASKS — {{ role }}-{{ NN }}

> This instance's **live board**. Keep it current — the PM turns it into the
> human-facing board and dashboard. Newest activity at the top of "Done".

## Now (in flight — one at a time)

- [ ] {{ the single task being executed right now }} — started {{ time }} — run `./runs/{{ YYYY-MM-DD }}T{{ HH-MM-SS }}/`

## Next (ordered backlog, pulled from PLAN.md)

- [ ] {{ task }} — acceptance: {{ condition }}
- [ ] {{ task }} — acceptance: {{ condition }}

## Blocked (waiting — bubbled up to PM)

- [ ] {{ task }} — blocked on: {{ what }} → `../pm/INBOX.md` I-{{ id }}

## Done (newest first)

- [x] {{ task }} — {{ result }} — {{ time }}

---

### Loop reminder

`pick top of Next → move to Now → execute → self-check vs BRIEF → move to Done`.
Log the session in `./runs/<YYYY-MM-DD>T<HH-MM-SS>/RUN.md`. If a task hits the autonomy edge,
move it to **Blocked** and append to `../pm/INBOX.md` — it bubbles up to the PM.
