---
name: project-manager-gather-intelligence
description: >
  Use this skill on demand to gather LIVE intelligence from running opencode servers:
  pull per-session tokens, cost, duration, agent and model over the REST API, aggregate
  them per agent/model/ticket, record the actuals on the matching tickets in the task
  store, and emit a measured cost baseline that calibrates project-manager-estimate-costs. Read-only
  against the server; the tickets are the accumulation point. opencode workflow utility.
---

# Live Intelligence Gathering Skill

## About this document
- **Kind:** skill (reusable capability, auto-loaded by opencode)
- **Read by:** any agent matching its description; **written by:** maintainers
- **Related:** part of the `project-manager-*` domain set; the measured counterpart of `project-manager-estimate-costs`
  (a-priori estimates) and the data source for its calibration; works with the task store
  (`task_*` tools). Market-side model intelligence (intelligence index, cost per task,
  context windows) comes from `research-artificial-analysis-models` — together the three
  skills form the cost loop: market rates → estimate → run → measure.

## Purpose

`project-manager-estimate-costs` prices a plan **before** it runs, from scope guesses. This skill closes
the loop with **actuals**: what did the agents really spend? The accumulated actuals become
the measured baseline per role/task-type so future estimates stop guessing.

## Position

Standalone, on-demand. Invoke after a fleet batch, at sprint close, or whenever the human
asks "what did this cost so far?". Never runs automatically.

## The live surface (opencode v1.18.x, verified against the harness client DTOs)

`GET http://127.0.0.1:<port>/session` returns per session:

```
{ id, slug, title, agent, parentID,
  time: { created, updated },        // epoch millis → duration = updated - created
  cost,                              // server-computed USD (sum over messages)
  tokens: { input, output, reasoning, cache: { read, write } } }
```

- Filter to one worktree/fleet job with `GET /session?directory=<path>`.
- `parentID` nests subagent sessions under their parent — **aggregate children into the
  parent** before attributing cost to a ticket.
- `cache.read/write` tokens are billed at different rates than plain input; do not
  recompute cost from tokens — trust the server's `cost` field.
- The port: spawn mode default `4096`; when the Eclipse harness runs, the Server view /
  connection preferences hold it.

## Workflow

1. **Collect** — query every configured server. PowerShell:
   ```powershell
   $sessions = (Invoke-RestMethod "http://127.0.0.1:4096/session")
   # flatten: children roll up into parents
   $roots = $sessions | Where-Object { -not $_.parentID }
   ```
   bash: `curl -s http://127.0.0.1:4096/session | jq '[.[] | select(.parentID == null)]'`
2. **Map session → ticket** — by convention the session `title` starts with the ticket id
   (`[T-014] implement store locking`). Sessions without a ticket prefix are fleet overhead;
   keep them in the aggregates, attribute them to `(unassigned)`.
3. **Record actuals on each ticket** — append one structured comment (no schema change; a
   first-class `cost` field waits until the data proves it):
   ```
   task_add_comment(project, ticket_id,
     comment: "telemetry: {\"session\":\"<id>\",\"agent\":\"build\",\"model\":\"<id>\","
            + "\"tokens\":{\"input\":I,\"output\":O,\"reasoning\":R,\"cache_read\":CR,\"cache_write\":CW},"
            + "\"cost_usd\":C,\"duration_min\":D}")
   ```
   Record once per session (check history first — idempotent re-runs must not double-count).
4. **Aggregate** — produce the rollup:
   - per ticket: total cost, tokens, duration, attempts (sessions count)
   - per agent and per model: totals and means
   - cost **per story point** and per task `type`/`role` — the estimator's leverage numbers
5. **Emit the baseline** — write a Markdown report (e.g.
   `.opencode/tasks/<project>/_reports/cost-baseline-<date>.md`) and attach it:
   `task_add_artifact(project, ticket_id="EP-…|none", kind="path", ref="<report>")`
   when a sprint/epic owns the batch, else just report in-channel.
6. **Calibrate** — hand the measured numbers to `project-manager-estimate-costs`: per-tier actuals
   replace rate-card guesses for the workload classes that have ≥3 samples. Flag the rest
   as still-guessed. Recommend de-escalations where a lower tier consistently sufficed.
   If the rate card itself is stale (prices drifted from the market), refresh it via
   `research-artificial-analysis-models` (live leaderboard: intelligence index, cost per
   task, context windows) — do not invent prices.

## Guardrails

- Read-only against the server; writes go only to the task store and report files.
- Costs are the server's truth — never re-derive from token counts.
- A crashed/retried session still counts (it spent money); mark retries in the comment.
- Do not push, do not start/stop servers, do not touch worktrees.

## Hand-off map

- Estimate needed before a run → `project-manager-estimate-costs` (this skill feeds it).
- Sprint close / review numbers → the `project-manager` agent consumes the report.
- Fleet telemetry automation (FleetRunner recording actuals on mergeBack) → ROADMAP
  "Standing"; until then this skill is the manual path.
