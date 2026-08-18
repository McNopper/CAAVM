# Hephaestus ROADMAP — the agentic harness

**Vision.** One repository. Intelligence lives in `.opencode/` (agents, skills) and MCP servers; the Eclipse plugin (`eclipse/`) is the harness: it hosts, services, and observes headless agents working in isolated git worktrees. The human uses Eclipse as PM + reviewer; agents self-organize via the task board. **Prime rule: never build in the plugin what Hephaestus already provides — and what it provides moves into the Maven/Eclipse world when we touch it.** Epics, sprints and milestones become **purely Maven-reflected** over the version-controlled task store.

## H0 — Consolidation ✅ (verified 2026-08-17: full reactor build green, ~181 Java + 105 JS)
1. Root `.gitignore` += `target/`, `node_modules/`, `*.log`. Copy `opencode-eclipse` → `eclipse/` (excluding `target/`, `node_modules/`, logs; drop `click-type.ps1`).
2. Path independence: `deploy-dev.ps1` (`-EclipseRoot`/`ECLIPSE_HOME`, default `C:\eclipse-cpp`), `debug-launch.ps1`/`shot.ps1` → `$env:TEMP\opencode\…`, docs re-pathed.
3. Port `opencode-viewer` features, drop the code: `ActivityTracker` in `client` (SSE → running tool + file path + thinking; ~10 tests), Server-view live activity + "Active files" node, derivation rules documented in `eclipse/ARCHITECTURE.md`.
4. Root `AGENTS.md` += `eclipse/` section (build/deploy/docs pointers); root README += subproject line; **old `eclipse/ROADMAP.md` content folded into this file** (single source of truth).
5. Verify: full reactor build from `eclipse/` (~181 Java + 105 JS), 9-jar deploy. Local commits, no push. Temp folders left for the user to delete.

## H1 — Task board on Maven storage (replaces the pm MCP) ✅ core landed 2026-08-17 (47 tests; stdio launcher live-verified)
1. ✅ **`tasks` bundle** (Eclipse-free, import-banned): `.opencode/tasks/` store — one Markdown+frontmatter file per task carrying the full ticket semantics from `.opencode/docs/contracts.md` (ids, status machine `product-backlog→sprint-backlog→in-progress→in-review→done`, blocked flag, roles, sprints/milestones/epics, artifacts, todos, history); `_meta.json` sidecar (id counters — never reused, sprint metadata); atomic self-claim (in-JVM lock + OS file lock + temp-rename writes); board state derived.
2. ✅ **`task_*` tools** on the existing harness MCP endpoint (`eclipse-build`, visible to every worktree session, zero per-worktree config) **and** over stdio — one tool surface, two transports. Live pm data migrated into `.opencode/tasks/test/`.
3. ✅ **Cutover commit**: deleted `mcp/pm/` + `mcp/base/`, retired `pm-reporter` + `pm-visualize`, rewrote AGENTS.md/README/skill/agent refs `pm_*` → `task_*`, ported the Python test data as fixtures (golden import-parity tests).
4. **FleetRunner v2**: `launch(task)` → worktree → directory-scoped session → role-mapped agent (domains.md map) → self-claim prompt → mergeBack.
5. ✅ **`tasks-tools` stdio launcher** (`eclipse/tasks-tools.ps1`, wired into `opencode.json`): same `task_*` tools over stdio for TUI-only sessions.
6. **Maven mojos `opencode-tasks:sync` / `opencode-tasks:plan`** (next up — the store schema is now stable): a plain `maven-plugin` module (`eclipse/mojo/opencode-tasks`) over the same store — `sync` validates/normalizes `.opencode/tasks/` at build time so CI and teammates converge (counter checks, LF normalization, schema lint), `plan` renders the sprint board as a Markdown/HTML report. Epics, sprint milestones and planning become **purely Maven-reflected**; the mojos never invoke a compiler.
7. **Dogfood cutover** (first thing next session): drop the `test` project from the store, seed the `hephaestus` project — a **Product Goal** statement first (Scrum 2020: the long-term target the backlog serves; epics are interim value chunks, *not* the final product — it rides in the project's `_meta.json`/seed doc until the schema earns a field), then epics mirroring this roadmap (H1 finish, H2 surfaces, H3 scale, …) plus their first concrete tickets, each sprint seeded with an explicit **Sprint Goal**; from then on the harness develops itself through its own board. Operational lesson (2026-08-17): MCP servers started by a session outlive config changes — after a cutover, restart sessions so tools rebind (this session's `pm_*` were still served by the pre-cutover Python zombie writing `mcp/pm/data/`; killed + data deleted, fresh sessions get `task_*` via `opencode.json`).

## H2 — Eclipse surfaces
1. **PM Board view**: kanban over the 5 statuses, sprint selector, blocked flags, artifact links; actions *Launch task* (→FleetRunner) and *Take over*; live store-watcher refresh.
2. **Fleet view**: jobs = task → session → worktree → state, per-job diff/log, takeover.
3. **Providers view logos**: Artificial Analysis SVGs (`https://artificialanalysis.ai/img/logos/<slug>_small.svg`) vendored at import into `eclipse/` icons + slug↔provider map; attribution in `THIRD-PARTY.md`; letter-badge fallback.
4. Server view: ActivityTracker live activity + active-files (from H0).

## H3 — Scale & depth
`ConnectionsManager` (plural) + `SWT.VIRTUAL` + request cache/throttle; session detail window (messages/parts/tools); SSE event-driven fleet completion (replaces polling).

## H4 — CDT integration & chat polish
Markers/editor bridge; chat abort, tool parts, copy-code; Chat-view context bindings.

## Standing
First-launch live check (MCP endpoint + `eclipse-build` registration + tool listing) rides with H0's deploy · refactor cadence every second session (backlog: spawn-lock off UI thread, spawn prefs page, `ISecurePreferences`, null-safe labels) · M4 `CodingAgent` port deferred until a second agent backend · second language pack ⇒ extract `tools.cpp` · session todos (`/session/{id}/todo`, `todo.updated` SSE) synchronize into the task store · **cost telemetry → task estimation:** record per-run token/cost actuals (opencode session tokens/cost fields + agent id) on the ticket when FleetRunner completes a task; the accumulated actuals calibrate `pm-estimate-costs` so task estimates move from scope-guess to measured baseline (manual path = `pm-gather-intelligence` skill, landed; automated path rides FleetRunner v2; tickets already reserve `story_points`; a `cost` field rides on the artifact/comment record until the schema earns it).
