# chat-web

A standalone, host-embeddable chat renderer: one static page (`web/chat.html`
plus `web/chat.js` and vendored libraries) that renders a chat transcript —
markdown, KaTeX math, mermaid diagrams, highlight.js code blocks — and exposes
a small JavaScript bridge.

It is plain HTML/JS/CSS with **no build step, no Node runtime dependency, no
OSGi and no Java**. Any host that can (a) serve the `web/` directory as static
files and (b) evaluate JavaScript strings in the page (Eclipse SWT
`Browser.execute`, a WebView `evaluateJavascript`, a test harness, …) can host
it. In this repository the Eclipse bundle `com.opencode.ide.chat` consumes it
at build time by copying `web/` into the plugin jar; that is one consumer, not
a requirement.

## Layout

```
components/chat-web/
  web/                     the component itself (serve this directory as-is)
    chat.html              entry page, loads everything below
    chat.js                renderer + host bridge (the contract lives here)
    markdown-it.min.js     markdown (vendored)
    mermaid.min.js         diagrams (vendored, esbuild IIFE)
    katex/                 math: katex.min.js, katex.min.css, fonts/
    hljs/                  code highlighting: highlight.min.js, cmake.min.js,
                           github.min.css + github-dark.min.css (theme pair)
  renderer-check.mjs       check: assets present, vendor libs actually render
  bridge-check.mjs         check: executes the bridge against a DOM shim
  package.json             `npm run check` runs both checks
```

## The bridge contract

The host talks to the page by evaluating JS that calls `window.__*` functions.
Payload arguments are **JSON strings** (single-quoted JS string literals of
JSON), e.g. `window.__appendUser("{\"text\":\"hello\"}")`. For convenience the
functions also accept a plain object literal — but the JSON-string form is the
authoritative contract (it is what the Java side emits).

Every bridge function is guarded: it returns `true` on success and `false` on
error, and errors are reported to the host (see `__javaReport` below) instead
of throwing silently inside `browser.execute()`.

### Host → page

| Call | Payload | Effect |
|---|---|---|
| `__setTheme(theme)` | plain string `"dark"` or `"light"` | Toggles `body.dark`/`body.light`, swaps the highlight.js stylesheet (`#hljs-light`/`#hljs-dark`), re-initialises mermaid lazily with the matching theme. |
| `__setNotice(text)` | plain string | Appends a centred, muted notice line to the transcript. |
| `__appendUser(json)` | `{"text": string}` | Appends a user bubble; text is rendered as markdown. |
| `__startAssistant(json)` | `{"mid": string}` | Appends an empty assistant bubble tagged `data-mid=mid` (idempotent: no-op if `mid` already exists). |
| `__appendDelta(json)` | `{"mid": string, "text": string}` | Streams raw (unformatted) text into the assistant bubble's `.stream-raw` element with a blinking cursor; creates the bubble if `__startAssistant` was not called. |
| `__setAssistantText(json)` | `{"mid": string, "text": string, "reasoning"?: string, "meta"?: string}` | Final authoritative render of the assistant bubble: markdown body (replacing streamed raw text), optional collapsible `reasoning` block, optional `meta` model label. |
| `__setMessages(json)` | JSON string of an array `[{"role":"user"\|"assistant","id":string,"text":string,"reasoning":string,"meta":string}, …]` | Replaces the whole transcript (history/resume load). |
| `__clear()` | none | Empties the transcript. |

Notes:
- `__appendDelta` / `__setAssistantText` identify the bubble by `mid`; a
  missing bubble is created on demand, so ordering is fault-tolerant.
- The streaming calls do **not** render markdown; only `__setAssistantText`
  does the final markdown render.
- `window.__linkClick(event)` also exists on `window`, but it is the page's
  internal click interceptor (exposed for the automated test) — hosts do not
  call it.

### Page → host

The page calls host-provided globals (define them before or after load; the
page tolerates them being absent, e.g. in a plain browser):

| Global | Meaning |
|---|---|
| `__javaReport(message: string)` | Progress/diagnostics channel. The page reports: `page-ready` on load (authoritative readiness signal — hosts flush queued renders on it), render confirmations (`user bubble rendered: …`, `assistant bubble rendered (N chars, meta=…)`, `notice rendered: …`, `history rendered (N entries)`, `theme set: …`, `mermaid initialised`, `mermaid blocks found: …`, `mermaid diagram rendered`), and failures: `JS ERROR in <fn>: <message>` from guarded bridge calls, `JS ERROR: …` from `window.onerror`, `JS REJECTION: …` from unhandled promise rejections, plus `KaTeX failed: …`, `mermaid … FAILED`, `highlight failed (…)` and `external link (no Java bridge): <url>`. |
| `__javaOpenExternal(url: string)` | A non-hash link was clicked. The page never navigates itself (that would destroy the transcript); the host must open the URL externally (OS browser). |

## Rendering rules (these rules ARE the contract)

- **Math is extracted before markdown.** `$…$`, `$$…$$`, `\(…\)` and `\[…\]`
  spans are replaced by private-use markers, markdown runs on the remainder,
  and KaTeX HTML (`renderToString`, `throwOnError: false`) is re-inserted
  afterwards. This survives shapes that a "markdown first, KaTeX auto-render"
  pipeline destroys: multi-line `$$` blocks (no stray `<br>`), `\(\)`/`\[\]`
  delimiters (markdown would eat the backslashes) and LaTeX `\\` line breaks.
  Currency (`$5 and $10`) is not math; fenced code and inline code are never
  touched by the math pass. Broken TeX degrades to a visible
  `<code class="error">` element, never silence.
- **Mermaid** fences (` ```mermaid `) are left for a diagram pass: the
  `<pre><code class="language-mermaid">` block is replaced by a `div.mermaid`
  and rendered via `mermaid.run({nodes:[…]})`. The bundled mermaid is an
  esbuild IIFE; the API is resolved through a `mermaidApi()` fallback chain:
  `window.mermaid` → `globalThis.mermaid` →
  `globalThis.__esbuild_esm_mermaid.default`. If no API is found the diagram
  source stays visible in an error element (and is reported) instead of an
  empty gap.
- **Syntax highlighting** via highlight.js for fenced code (unknown/untagged
  languages still get code styling without token colours; the `cmake` language
  registers from its own file). The light/dark highlight.js stylesheets are
  toggled by `__setTheme`.
- **XSS hardening:** markdown-it runs with `html: false` (raw HTML in model
  output is escaped, not interpreted); mermaid runs with
  `securityLevel: "strict"`; all interpolations into markup go through HTML
  escaping.
- **Single scrolling Container:** `html, body { overflow: hidden }` — only
  `#chat` scrolls, so an embedded view never shows a double scrollbar.
- **Links never navigate the page** — every non-`#` link click is intercepted
  and handed to `__javaOpenExternal`.

## Running the checks

Requires Node.js on PATH (only for the checks — the component itself is
build-free):

```
node renderer-check.mjs   # assets present; markdown-it/KaTeX/hljs really render (43 checks)
node bridge-check.mjs     # executes chat.js in a VM with a DOM shim and drives
                          # the bridge exactly as a host would (54 checks)
```

or `npm run check` for both. Exit code 0 = all checks pass.

`bridge-check.mjs` honours a `WEB_DIR` environment variable to run against an
alternative copy of the page (used to prove the checks fail when the bridge is
broken).

## Consuming the component

- Serve `web/` over any static file mechanism (the Eclipse consumer runs a
  tiny local HTTP server against a directory resolver; a jar resource
  extraction, WebView `loadFile`/`loadDataWithBaseURL`, or a CDN copy all work
  equally well — relative asset paths are all below `web/`).
- Provide `__javaReport` and `__javaOpenExternal` in the page context.
- Wait for the `page-ready` report, then drive the UI with the calls above.
