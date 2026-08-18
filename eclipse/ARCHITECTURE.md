# Architecture — opencode-eclipse

The modular architecture of the **agentic C++ harness**. This document defines the separation
axes, the current state (verified against manifests/imports), and the migration steps toward the
target. It is the reference for every refactor session (see ROADMAP "process rules").

## The four separation axes

Code is separated along four independent axes. A module's position on each axis is a **deliberate,
reviewed decision**, not an accident:

1. **Eclipse vs. non-Eclipse (Java):** pure Java must stay usable *outside* Eclipse/OSGi — as a
   plain library, in a CLI, or under another IDE. Eclipse APIs (SWT/JFace/Workbench/preferences/
   equinox) are allowed only in the UI layer.
2. **Java vs. non-Java:** the web renderer (HTML/JS/CSS + KaTeX/highlight.js/mermaid) is a
   standalone front-end component with a **versioned bridge contract** — reusable in any host
   (Eclipse Browser, VS Code webview, plain web page) without Java.
3. **Development environment packs (C++ today, Python/other later):** everything language-specific
   (toolchains, build systems, linters, debuggers) lives behind the **`ToolProvider` SPI**. The
   harness core never knows which language it is driving.
4. **Coding-agent backend (opencode today, others later):** all agent interaction goes through the
   core client layer. The seam for a second backend is the client interface + event model; we do
   NOT pre-build a generic abstraction — we keep the boundary clean so a `CodingAgent` port can be
   extracted when a second implementation actually arrives (rule: second implementation justifies
   the abstraction; until then we document, not speculative-build).

## Current state (after migrations M1–M3, landed 2026-08-15)

| Module | Eclipse-free? | Non-Java assets | Language pack | Agent backend | Verdict |
|---|---|---|---|---|---|
| `com.opencode.ide.client` (+ tests) | ✅ **enforced by build** (pwsh scan fails the build on `org.eclipse.`/`org.osgi.` imports) | — | neutral | opencode (HTTP+SSE, DTOs, ChatRequests/McpRequests, server launcher; hardened error semantics + URL validation) | M1 done |
| `com.opencode.ide.core` (+ tests) | ❌ (by design — the **Eclipse adapter**): preferences, activator + `ClientLog` bridge, `ProjectContext` service tracking, `OpencodeConnection` service, **MCP registration component** | — | neutral | — | M1 done |
| `com.opencode.ide.tools` (+ tests) | ✅ enforced by build | — | SPI (`ToolProvider`, `McpDispatcher`) + built-in C++ pack (`tools.cpp`) | neutral | M3 done |
| `com.opencode.ide.mcp` (+ tests) | ✅ mostly (OSGi DS lifecycle only; publishes `McpInfo` as a service) | — | — (delegates to tools) | neutral | M3 done |
| `components/chat-web` | n/a (non-Java) | ✅ chat.html, chat.js, markdown-it, hljs, KaTeX, mermaid + checks (43 renderer / 54 bridge / **8 mermaid in real headless Edge**) + standalone README | neutral | renders opencode data only | M2 done |
| `com.opencode.ide.chat` | ❌ (by design — Eclipse host for chat-web: `ChatPage` + `ChatSessionController`, SWT-free) | consumes chat-web at build time (resources-plugin copy → jar `web/`) | neutral | renders opencode data | M2 done |
| `com.opencode.ide.git` | ✅ yes (zero Eclipse/OSGi imports) | — | neutral | neutral | good |
| `com.opencode.ide.fleet` (+ tests) | ✅ enforced by build | — | neutral | **the fleet engine**: `FleetRunner` drives client + git worktrees headless (submit → poll → mergeBack) | good (Phase 15 engine) |
| `com.opencode.ide.ui` / `.cdt` | ❌ (by design — Eclipse layer; ui gained `ViewLoadSupport` + default-scheme key bindings) | — | cdt: C++ bridge | neutral | good |

## Target layout — now the actual layout (M1–M3 landed)

```
┌────────────────────────────── non-Eclipse, reusable ──────────────────────────────┐
│  com.opencode.ide.client     opencode REST/SSE client, DTOs, ChatRequests/        │
│  (OSGi bundle, pure Java)    McpRequests, server launcher — Eclipse imports       │
│                              banned at build time; usable as a plain library      │
│  com.opencode.ide.tools      ToolProvider SPI + JSON-RPC dispatch + the built-in  │
│  (OSGi bundle, pure Java)    C++ pack (tools.cpp) — Eclipse imports banned        │
│  com.opencode.ide.fleet      Headless fleet engine: FleetRunner drives the client │
│  (OSGi bundle, pure Java)    + git worktrees (submit → poll → mergeBack) — the    │
│                              layer the future Fleet view/scheduler will call     │
│  components/chat-web         the web renderer as a standalone component: web      │
│  (non-Java, no OSGi)         assets + checks + README; hostable in any host       │
└────────────────────────────────────────────────────────────────────────────────────┘
┌──────────────────── Eclipse layer (thin adapters + UI) ───────────────────────────┐
│  com.opencode.ide.core    Eclipse adapter: preferences (→ISecurePreferences       │
│  (backlog)), activator + ClientLog bridge, ProjectContext service tracking,       │
│  OpencodeConnection; depends on client                                            │
│  com.opencode.ide.chat    Eclipse host for chat-web: ChatPage (Browser wiring)    │
│  + ChatSessionController (SWT-free); consumes the component at build time         │
│  com.opencode.ide.mcp     MCP HTTP endpoint + DS lifecycle only; depends on tools │
│  com.opencode.ide.ui/.cdt views, perspective, CDT adapter                         │
└────────────────────────────────────────────────────────────────────────────────────┘
Language packs (axis 3):  tools.cpp.CppToolProvider (built-in) · future packs = new
                          ToolProvider impls (own bundle, depend on tools only)
Agent backends (axis 4):  opencode HttpOpencodeClient (today) · M4 below
```

### Migration status & follow-ups

- **M1 ✅ (2026-08-15):** `client` split out (69 tests moved, core.tests deleted); Eclipse-import
  ban enforced in the client pom (`-DskipEclipseBan=true` skips). `OpencodeServerLauncher` and
  `OpencodeEventStream` are public in client (core constructs them directly — no extra
  interfaces invented); only `HttpOpencodeClient`/`Auth` stay internal. Follow-up candidates:
  eclipse-ize only when needed — `ClientLog` adapter lives in core's activator.
- **M2 ✅ (2026-08-15):** `components/chat-web` extracted (70 web files, checks moved + green,
  standalone README = consumer doc); chat bundle copies the web assets into its jar at
  process-resources; jar packaging verified identical. The bridge contract section below is now
  **verified against the implementation** (earlier drafts had wrong entry-point names).
- **M3 ✅ (2026-08-15):** SPI + dispatcher + C++ pack moved to `tools` (23 tests), mcp keeps
  endpoint + DS (6 tests); Eclipse-import ban enforced in the tools pom. When a second language
  pack arrives, extract `tools.cpp` into its own bundle depending on `tools` only.
- **M4 — deferred (unchanged):** when a second coding-agent backend is seriously evaluated,
  extract a `CodingAgent` port (createSession/sendMessage/event stream/cancel) from
  `OpencodeClient` + `OpencodeEventStream`. Until then: keep client's API small; do not
  speculative-build the abstraction.

## Live activity derivation (`client.activity` — ported from the retired opencode-viewer)

`ActivityTracker` consumes the same `/event` SSE stream the Server view subscribes to and derives:
per-session `running` (from `session.status` busy/retry vs `session.idle`/deleted), `thinking`
(`message.part.updated` with `part.type=="reasoning"` on, any other non-tool part off), tool
invocations (`part.type=="tool"`: name from `part.tool`, state from `part.state.status`
running/completed/error, null ⇒ running), and the **active-files map** — a file (first non-blank
of `part.input.filePath|path|file|absolutePath`) is listed while its tool is RUNNING and removed
on COMPLETED/ERROR. Snapshots are immutable; listeners fire only on real changes. The ui Server
view renders `thinking…` / `tool: <name> — <file>` labels and the "Active files" node from it.

## The web bridge contract (chat-web's public API)

The renderer is hostable anywhere that can (a) serve static files over HTTP and (b) call JS with
string arguments. Contract (enforced by `bridge-check.mjs`, 54 checks):

- **Host → page** (via `ChatScripts`; payloads are JSON *string literals* — objects tolerated;
  every entry point is guarded, returns `true`/`false`, reports errors via
  `JS ERROR in <fn>: …` so nothing fails silently the way `Browser.execute()` does):
  `__setTheme(theme)` and `__setNotice(text)` take **plain strings**; data calls are
  `__appendUser({text})`, `__startAssistant({mid})`,
  `__setAssistantText({mid, text, reasoning?, meta?})`,
  `__appendDelta({mid, text})` (raw text stream + cursor, no markdown),
  `__setMessages([…])` (history), `__clear()`.
  (`__linkClick(event)` exists but is the page's internal test-exposed click interceptor.)
- **Page → host:** `__javaReport(type, detail)` — `page-ready` flush signal, render
  confirmations (`user/assistant bubble rendered…`, `history rendered (N entries)`,
  `mermaid initialised/diagram rendered`, `KaTeX failed…`), JS errors; and
  `__javaOpenExternal(url)` for link clicks.
- **Rendering rules that ARE the contract:** math extracted before markdown (KaTeX; pass skips
  fenced/inline code and currency like `$5`; `throwOnError:false` → visible `<code class="error">`
  degradation), mermaid via the `mermaidApi()` fallback chain (`window.mermaid` →
  `globalThis.mermaid` → `globalThis.__esbuild_esm_mermaid.default`; missing mermaid leaves the
  visible source, not a gap), highlight.js for code fences with the `#hljs-light`/`#hljs-dark`
  theme pair toggled by `__setTheme` (which also resets mermaid for themed re-init), markdown
  `html:false` + mermaid `securityLevel:strict` (XSS hardening), single scrolling container.
- **Checks:** `components/chat-web` runs `renderer-check.mjs` (43) + `bridge-check.mjs` (54) +
  `mermaid-check.mjs` (8 — drives the real page in **headless Microsoft Edge** via
  `puppeteer-core`: diagrams must render to SVG, broken sources degrade visibly, no silent drops;
  SKIPs when Edge/puppeteer-core are absent) — all wired into the chat bundle's `mvn verify`
  (`-DskipNodeChecks=true` skips); `bridge-check.mjs` honours a `WEB_DIR` env override.

## Review checklist (every refactor/PR session)

1. Did any `org.eclipse.*`/`org.osgi.*` import creep into an Eclipse-free module? (build check)
2. Did any language-specific logic (cmake/clang/msvc paths…) escape a `ToolProvider`?
3. Did the web bridge contract change without a `bridge-check.mjs` update?
4. Did any new opencode DTO/endpoint leak outside the client layer?
5. Is every new module position on the four axes documented here?
