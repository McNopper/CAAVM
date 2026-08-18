# OpenCode IDE plugin — install & run guide

Eclipse plugin integrating [opencode](https://opencode.ai) into the Eclipse CDT IDE
(`<eclipse-install>` — default `C:\eclipse-cpp`; the deploy scripts accept an
`-EclipseRoot` argument or the `ECLIPSE_HOME` env var; Eclipse 4.40 / Java 21 / CDT 12.5).
Source lives in the repo's `eclipse/` folder.

## Prerequisites

- **opencode** installed and on PATH (`opencode --version` → 1.18.x). Tested with 1.18.16+.
- A JDK 17+ on the machine (the `build.ps1` wrapper auto-detects one;
  `JAVA_HOME` does not have to be valid).
- **Node.js on PATH** — only needed for the chat web renderer/bridge checks that run inside
  `mvn verify` (skip with `-DskipNodeChecks=true` if you just want jars).
- **WebView2 Runtime** (preinstalled on Windows 10/11) for the Chat view.
- **Toolchains (all optional — auto-detected, agents get install hints for anything missing):**
  - **MSYS2** at `C:\msys64` with one or more of `clang64` / `mingw64` / `ucrt64`
    (each env should have `cmake` + `ninja`; `clang-tidy`/`clang-format` in clang64/mingw64).
  - **MSVC** — Visual Studio (2022/2026) is found via `vswhere`; builds use CMake's
    Visual Studio generator (no developer prompt needed).
  - **Cppcheck** (standalone install, e.g. `C:\Program Files\Cppcheck`) for `lint_run`.
  - **gdb** — *not present on the reference machine*; `debug_batch` reports an install hint
    (`pacman -S gdb` in the MSYS2 env) until installed.

## Build (always)

Full reactor (heavy; also builds the p2 site):

```powershell
cd eclipse   # from the repo root
.\build.ps1 clean verify
```

Scoped build during iteration (repeat `-pl`, never commas; adjust to the modules you touched):

```powershell
.\build.ps1 -pl bundles/com.opencode.ide.client -pl bundles/com.opencode.ide.client.tests `
            -pl bundles/com.opencode.ide.core -pl bundles/com.opencode.ide.ui `
            -pl bundles/com.opencode.ide.chat -pl bundles/com.opencode.ide.chat.tests `
            -pl bundles/com.opencode.ide.cdt -pl bundles/com.opencode.ide.git `
            -pl bundles/com.opencode.ide.git.tests -pl bundles/com.opencode.ide.tools `
            -pl bundles/com.opencode.ide.tools.tests -pl bundles/com.opencode.ide.mcp `
            -pl bundles/com.opencode.ide.mcp.tests clean verify
```

Both run the 137 Java tests; `verify` also runs the 97 Node checks (against
`components/chat-web`) when Node is available (`-DskipNodeChecks=true` to skip). Produces
plugin JARs in `bundles\<name>\target\` and (full build) a p2 update site in
`releng\com.opencode.ide.repository\target\repository\`.

---

## Three ways to run the plugin

### Option A — `dropins/` (fastest, no setup) ★ for iterating

Copies the freshly built plugin JARs straight into the Eclipse dropins folder.

```powershell
.\build.ps1 clean verify
.\deploy-dev.ps1            # copies the 8 JARs to <eclipse-install>\dropins\opencode-ide\plugins\
```

Then **(re)start Eclipse CDT** and open the **OpenCode** perspective.

- Re-run both lines after any code change, then restart Eclipse.
- If the perspective/views don't refresh after a change, run Eclipse once with `-clean`
  (add a line `-clean` near the top of `<eclipse-install>\eclipse.ini`, start once, remove it).
- **Undo:** delete `<eclipse-install>\dropins\opencode-ide\` and restart — plugin is gone.
- No debugging/breakpoints with this route.

### Option B — p2 install (stable / "production")

Install the built p2 repository into Eclipse once:

1. In Eclipse CDT: **Help → Install New Software…**
2. **Add…** → **Local** → select
   `<repo>/eclipse/releng/com.opencode.ide.repository/target/repository`
3. Select **OpenCode IDE**, finish, restart.

To update after a rebuild: Help → **Installation Details** → uninstall, then reinstall,
or just re-run the repository and it will offer an update. For frequent changes prefer Option A.

### Option C — PDE runtime launch (dev + debugging)

The proper Eclipse dev workflow. **PDE is not installed in `eclipse-cpp` today**, so it's a
one-time setup:

1. *Help → Install New Software → 2026-06 repo* (`https://download.eclipse.org/releases/2026-06/`)
   → install:
   - **Eclipse Plugin Development Environment**
   - **Eclipse Java Development Tools**
   - **Maven Integration for Eclipse (m2e)** (+ the m2e Tycho/PDE connector if prompted)
2. Restart Eclipse.
3. *File → Import → Maven → Existing Maven Projects* → select the repo's `eclipse` folder.
4. **Run → Debug As → Eclipse Application** — launches a 2nd Eclipse with your workspace
   plugins live. Set breakpoints in `AgentsView`, `ProvidersView`, `HttpOpencodeClient`, etc.
   Relaunch picks up code changes; no restart/copy needed.

---

## Using the plugin

1. **Start an opencode server** (one of):
   - *Connect mode* (default): in a terminal run
     ```
     opencode serve --hostname 127.0.0.1 --port 4096
     ```
     (If you set `OPENCODE_SERVER_PASSWORD`, also enter it in the preference page below.)
   - *Spawn mode*: in **Window → Preferences → OpenCode** set **Mode = SPAWN**. The plugin
     starts and manages an `opencode serve` child process itself (resolves the binary from
     the preference or PATH; kills the process tree on stop).
2. **Window → Perspective → Open Perspective → Other… → OpenCode**.
3. The **Server** view (left) shows the connection with agents and live sessions (subagents
   nested; thinking/running-tool indicator). The **Providers** view (bottom) lists all models
   with filter + column sorting. Use the views' **Refresh** action to re-query.
4. The **Chat** view (right) is a native markdown chat: pick an agent + model (+ **variant** for
   models that expose them, e.g. `high`/`thinking`; `(default)` omits it), type a prompt
   (**ENTER** sends, **Shift+ENTER** = newline). Replies render markdown, **LaTeX math**
   (`$x^2$`, `$$…$$`), and **syntax-highlighted code** (c/cpp/cmake/…) with streaming text while
   the model works. Toolbar: **New Session**. Double-clicking a model in Providers or a session in
   the Server view opens a chat window pre-set to it / resuming it.
   - Requires **WebView2**; the view shows a hint if unavailable.
   - The plugin tells the model what the view can render (markdown/math/code fences) via a
     per-request system prompt — toggle in *Preferences → OpenCode → Advertise rendering*.
   - **Known issue:** mermaid diagrams currently render as an error box with the diagram source
     (fix in verification); everything else renders.
5. If the server URL or credentials differ, set them in
   **Window → Preferences → OpenCode**, then hit **Refresh**.
6. On startup the plugin also starts a local **MCP endpoint** for agents
   (log line: `eclipse-build MCP listening on http://127.0.0.1:<port>/mcp`) exposing
   cmake build/test, run, gdb-batch debug, clang-tidy/cppcheck lint and clang-format tools
   across the detected toolchains (MSVC + MSYS2 clang64/mingw64/ucrt64).

## Connection modes at a glance

| Mode | Where the server comes from | Preference fields |
|---|---|---|
| **CONNECT** (default) | You start `opencode serve` | Server URL, Username, Password |
| **SPAWN** | Plugin starts/owns `opencode serve` | (optional) opencode binary, hostname, port, Password |

## Troubleshooting

- **Perspective not visible** after a dropins/p2 update → start Eclipse once with `-clean`.
- **Views show "Error: …"** → check the server is reachable:
  `Invoke-RestMethod http://127.0.0.1:4096/global/health` (should report `healthy=true`).
- **Spawn mode: "opencode binary not found"** → set the binary path in Preferences → OpenCode
  (e.g. `C:\Users\<you>\AppData\Roaming\npm\node_modules\opencode-ai\bin\opencode.exe`).
- **Chat renders blank / a feature (math, mermaid) stops working** → the chat page reports every
  render and JS error to the Eclipse log as `[chat-page] …` messages — check the **Error Log**
  view (*Window → Show View → Error Log*) or the workspace `.metadata\.log`; those markers say
  exactly which bridge call or renderer stage failed.
- Errors are logged to the **Error Log** view (*Window → Show View → Error Log*).

## Uninstall

- **dropins:** delete `<eclipse-install>\dropins\opencode-ide\` and restart.
- **p2:** Help → Installation Details → select **OpenCode IDE** → Uninstall.
