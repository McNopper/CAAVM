# Hephaestus ROADMAP — the agentic harness

**Vision.** One repository. Intelligence lives in `.opencode/` (agents, skills) and MCP servers; the Eclipse plugin (`eclipse/`) is the harness: it hosts, services, and observes headless agents working in isolated git worktrees. The human uses Eclipse as PM + reviewer; agents self-organize via the task board. **Prime rule: never build in the plugin what Hephaestus already provides — and what it provides moves into the Maven/Eclipse world when we touch it.**

## H0 — Consolidation (in progress)
1. Root `.gitignore` += `target/`, `node_modules/`, `*.log`. Copy `opencode-eclipse` → `eclipse/` (excluding `target/`, `node_modules/`, logs; drop `click-type.ps1`).
2. Path independence: `deploy-dev.ps1` (`-EclipseRoot`/`ECLIPSE_HOME`, default `C:\eclipse-cpp`), `debug-launch.ps1`/`shot.ps1` → `$env:TEMP\opencode\…`, docs re-pathed.
3. Port `opencode-viewer` features, drop the code: `ActivityTracker` in `client` (SSE → running tool + file path + thinking; ~10 tests), Server-view live activity + "Active files" node, derivation rules documented in `eclipse/ARCHITECTURE.md`.
4. Root `AGENTS.md` += `eclipse/` section (build/deploy/docs pointers); root README += subproject line; **old `eclipse/ROADMAP.md` content folded into this file** (single source of truth).
5. Verify: full reactor build from `eclipse/` (~181 Java + 105 JS), 9-jar deploy. Local commits, no push. Temp folders left for the user to delete.

## H1 — Task board on Maven storage (replaces the pm MCP)
1. **`tasks` bundle** (Eclipse-free, import-banned): `.opencode/tasks/` store — one Markdown+frontmatter file per task carrying the full ticket semantics from `.opencode/docs/contracts.md` (ids, status machine `product-backlog→sprint-backlog→in-progress→in-review→done`, blocked flag, roles, sprints/milestones/epics, artifacts, todos, history); atomic self-claim (filelock); board state derived.
2. **`task_*` tools** on the existing harness MCP endpoint (visible to every worktree session, zero per-worktree config); Board view uses the same service in-process.
3. **Cutover commit**: delete `mcp/pm/`, retire `pm-reporter` + `pm-visualize`, rewrite AGENTS.md/skill refs `pm_*` → `task_*`, port the Python test data as fixtures.
4. **FleetRunner v2**: `launch(task)` → worktree → directory-scoped session → role-mapped agent (domains.md map) → self-claim prompt → mergeBack.
5. `tasks-tools` stdio jar (same module) for TUI-only use; `opencode-tasks:sync`/`plan` Maven mojos deferred until the store schema is stable.

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
First-launch live check (MCP endpoint + `eclipse-build` registration + tool listing) rides with H0's deploy · refactor cadence every second session (backlog: spawn-lock off UI thread, spawn prefs page, `ISecurePreferences`, null-safe labels) · M4 `CodingAgent` port deferred until a second agent backend · second language pack ⇒ extract `tools.cpp` · session todos (`/session/{id}/todo`, `todo.updated` SSE) synchronize into the task store.
