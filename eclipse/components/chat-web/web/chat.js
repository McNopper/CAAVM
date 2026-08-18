"use strict";

function escapeHtml(value) {
  return String(value).replace(/[&<>"]/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;" }[c]));
}

// Syntax highlighting for fenced code blocks. The user works on C/C++, so those
// matter most; the bundled highlight.js build covers c, cpp, makefile, bash,
// python, java, json, xml, sql, ... and cmake is registered from its own file.
// Returning markup that starts with "<pre" tells markdown-it to use it verbatim.
function highlightFence(code, lang) {
  const language = String(lang || "").toLowerCase().trim();
  if (language === "mermaid") {
    return ""; // rendered as a diagram later - keep markdown-it's default markup
  }
  if (window.hljs && language && window.hljs.getLanguage(language)) {
    try {
      const html = window.hljs.highlight(code, { language: language, ignoreIllegals: true }).value;
      return "<pre><code class=\"hljs language-" + escapeHtml(language) + "\">" + html + "</code></pre>";
    } catch (e) {
      report("highlight failed (" + language + "): " + (e && e.message ? e.message : String(e)));
    }
  }
  // unknown/untagged language: no token colours, but keep the code styling
  return "<pre><code class=\"hljs" + (language ? " language-" + escapeHtml(language) : "")
    + "\">" + escapeHtml(code) + "</code></pre>";
}

const md = window.markdownit({
  html: false, linkify: true, breaks: true, typographer: false,
  highlight: highlightFence
});
let mermaidReady = false;
function ensureMermaid() {
  if (mermaidReady) return;
  mermaidReady = true;
  try {
    mermaidApi().initialize({
      startOnLoad: false,
      securityLevel: "strict",
      theme: document.body.classList.contains("dark") ? "dark" : "default"
    });
    report("mermaid initialised");
  } catch (e) {
    // never silent: a missing/broken mermaid used to just drop the diagram
    report("mermaid init FAILED: " + (e && e.message ? e.message : String(e)));
  }
}

/** The bundled mermaid is an esbuild IIFE; it exposes itself on globalThis. */
function mermaidApi() {
  return window.mermaid
    || (typeof globalThis !== "undefined" && globalThis.mermaid)
    || (typeof globalThis !== "undefined" && globalThis.__esbuild_esm_mermaid
        && globalThis.__esbuild_esm_mermaid.default)
    || null;
}

// ---- math ----------------------------------------------------------------
// Math is extracted BEFORE markdown and re-inserted as KaTeX HTML afterwards.
// Rendering math after markdown (KaTeX auto-render walking the DOM) is broken
// for real model output, verified against a live server:
//   * "$$\n x = ... \n$$"  -> breaks:true puts <br> inside, splitting the text
//                             nodes, so the delimiters never match -> raw "$$"
//   * "\(x^2\)" / "\[y\]"  -> markdown eats the backslash escapes -> "(x^2)"
//   * "$$a \\ b$$"         -> markdown collapses "\\" -> LaTeX line break lost
// Extracting first also keeps _, *, ` inside formulas away from markdown.
const MATH_OPEN = "\uE000";   // private-use markers: markdown passes them through
const MATH_CLOSE = "\uE001";

function atLineStart(text, i) {
  return i === 0 || text[i - 1] === "\n";
}

// ``` or ~~~ fenced block -> index just past the closing fence
function fenceEnd(text, i) {
  const marker = text[i];
  let run = 0;
  while (text[i + run] === marker) run++;
  if (run < 3) return -1;
  const fence = marker.repeat(run);
  let scan = text.indexOf("\n", i);
  if (scan < 0) return text.length;
  while (scan < text.length) {
    const lineStart = scan + 1;
    const lineEnd = text.indexOf("\n", lineStart);
    const line = text.slice(lineStart, lineEnd < 0 ? text.length : lineEnd);
    if (line.trim().startsWith(fence)) return lineEnd < 0 ? text.length : lineEnd + 1;
    if (lineEnd < 0) break;
    scan = lineEnd;
  }
  return text.length;
}

// `code` span -> index just past the closing backtick run
function inlineCodeEnd(text, i) {
  let run = 0;
  while (text[i + run] === "`") run++;
  const closing = "`".repeat(run);
  const end = text.indexOf(closing, i + run);
  return end < 0 ? -1 : end + run;
}

// closing "$" of an inline span, or -1 (skips currency: "$5 and $10")
function inlineDollarEnd(text, i) {
  const next = text[i + 1];
  if (next === undefined || /\s/.test(next)) return -1;
  for (let j = i + 1; j < text.length; j++) {
    const ch = text[j];
    if (ch === "\\") { j++; continue; }
    if (ch === "\n" && text[j + 1] === "\n") return -1; // don't cross paragraphs
    if (ch === "$") {
      if (/\s/.test(text[j - 1])) continue;      // "... $" is not a closer
      if (/\d/.test(text[j + 1] || "")) return -1; // "$10" -> currency, not math
      return j;
    }
  }
  return -1;
}

/** Replaces math spans with markers. @return {source, spans} */
function extractMath(text) {
  const spans = [];
  let out = "";
  let i = 0;
  const mark = (tex, display) => {
    spans.push({ tex: tex, display: display });
    return MATH_OPEN + (spans.length - 1) + MATH_CLOSE;
  };
  while (i < text.length) {
    const ch = text[i];
    if ((ch === "`" || ch === "~") && atLineStart(text, i)) {
      const end = fenceEnd(text, i);
      if (end > 0) { out += text.slice(i, end); i = end; continue; }
    }
    if (ch === "`") {
      const end = inlineCodeEnd(text, i);
      if (end > 0) { out += text.slice(i, end); i = end; continue; }
    }
    if (ch === "\\" && (text[i + 1] === "[" || text[i + 1] === "(")) {
      const display = text[i + 1] === "[";
      const close = display ? "\\]" : "\\)";
      const end = text.indexOf(close, i + 2);
      if (end > 0) { out += mark(text.slice(i + 2, end), display); i = end + 2; continue; }
    }
    if (ch === "$") {
      if (text[i + 1] === "$") {
        const end = text.indexOf("$$", i + 2);
        if (end > 0) { out += mark(text.slice(i + 2, end), true); i = end + 2; continue; }
      } else {
        const end = inlineDollarEnd(text, i);
        if (end > 0) { out += mark(text.slice(i + 1, end), false); i = end + 1; continue; }
      }
    }
    out += ch;
    i++;
  }
  return { source: out, spans: spans };
}

/** Puts the KaTeX-rendered math back into the markdown HTML. */
function restoreMath(html, spans) {
  if (spans.length === 0) return html;
  const pattern = new RegExp(MATH_OPEN + "(\\d+)" + MATH_CLOSE, "g");
  return html.replace(pattern, (match, index) => {
    const span = spans[Number(index)];
    if (!span) return match;
    try {
      return window.katex.renderToString(span.tex, {
        displayMode: span.display,
        throwOnError: false,
        strict: false
      });
    } catch (e) {
      report("KaTeX failed: " + (e && e.message ? e.message : String(e)));
      const raw = span.display ? "$$" + span.tex + "$$" : "$" + span.tex + "$";
      return "<code class=\"error\">" + escapeHtml(raw) + "</code>";
    }
  });
}

function renderMarkdown(el, text) {
  const extracted = extractMath(text || "");
  el.innerHTML = restoreMath(md.render(extracted.source), extracted.spans);
  // mermaid diagrams
  const mermaidBlocks = el.querySelectorAll("pre > code.language-mermaid");
  if (mermaidBlocks.length > 0) {
    ensureMermaid();
    const mermaid = mermaidApi();
    report("mermaid blocks found: " + mermaidBlocks.length + ", api: " + (mermaid ? "yes" : "MISSING"));
    mermaidBlocks.forEach(code => {
      const div = document.createElement("div");
      div.className = "mermaid";
      div.textContent = code.textContent;
      code.parentElement.replaceWith(div);
      if (!mermaid) {
        // visible instead of an empty gap, and reported to the Eclipse log
        div.className = "error";
        div.textContent = "mermaid unavailable - diagram source:\n" + div.textContent;
        return;
      }
      try {
        const result = mermaid.run({ nodes: [div] });
        if (result && typeof result.then === "function") {
          result.then(() => report("mermaid diagram rendered")).catch(err => {
            div.className = "error";
            div.textContent = "mermaid: " + String(err && err.message ? err.message : err);
            report("mermaid render FAILED: " + String(err && err.message ? err.message : err));
          });
        }
      } catch (e) {
        div.className = "error";
        div.textContent = "mermaid: " + (e && e.message ? e.message : String(e));
        report("mermaid run threw: " + (e && e.message ? e.message : String(e)));
      }
    });
  }
}

const chatEl = document.getElementById("chat");
function scrollBottom() { chatEl.scrollTop = chatEl.scrollHeight; }
function report(msg) { try { if (typeof window.__javaReport === "function") window.__javaReport(msg); } catch (e) {} }

function addUser(text) {
  const wrap = document.createElement("div"); wrap.className = "msg user";
  const bubble = document.createElement("div"); bubble.className = "bubble";
  renderMarkdown(bubble, text);
  wrap.appendChild(bubble); chatEl.appendChild(wrap); scrollBottom();
  report("user bubble rendered: " + text.slice(0, 60));
}

function addAssistant(messageId, reasoningText) {
  const wrap = document.createElement("div"); wrap.className = "msg assistant";
  wrap.dataset.mid = messageId || "";
  const bubble = document.createElement("div"); bubble.className = "bubble";
  if (reasoningText && reasoningText.trim().length > 0) {
    const details = document.createElement("details"); details.className = "reasoning";
    const summary = document.createElement("summary"); summary.textContent = "reasoning";
    const rbody = document.createElement("div"); renderMarkdown(rbody, reasoningText);
    details.appendChild(summary); details.appendChild(rbody); bubble.appendChild(details);
  }
  const body = document.createElement("div"); body.className = "body";
  bubble.appendChild(body);
  wrap.appendChild(bubble); chatEl.appendChild(wrap); scrollBottom();
  return { wrap, bubble, body };
}

function findAssistant(messageId) {
  if (!messageId) return null;
  return chatEl.querySelector('.msg.assistant[data-mid="' + cssEscape(messageId) + '"]') || null;
}
function cssEscape(s) { return (window.CSS && CSS.escape) ? CSS.escape(s) : s.replace(/[^a-zA-Z0-9_-]/g, "_"); }

// ---- Java bridge (called via browser.execute) ----
// Payloads are JSON *strings* (Java: ChatScripts). Objects are accepted too, so a
// call built by hand (window.__appendUser({...})) cannot silently do nothing.
function payload(arg) {
  if (typeof arg === "string") return JSON.parse(arg);
  if (arg && typeof arg === "object") return arg;
  return {};
}

// Every bridge call is guarded: JS errors are reported to Java (and the call
// returns false) instead of failing silently inside browser.execute().
function guard(name, fn) {
  return function (arg) {
    try {
      return fn(arg);
    } catch (e) {
      report("JS ERROR in " + name + ": " + (e && e.message ? e.message : String(e)));
      return false;
    }
  };
}

window.onerror = function (message, source, line, col) {
  report("JS ERROR: " + message + " @" + line + ":" + col);
  return false;
};
window.addEventListener("unhandledrejection", function (e) {
  report("JS REJECTION: " + String(e && e.reason ? e.reason : e));
});

window.__setTheme = guard("__setTheme", function (theme) {
  const name = typeof theme === "string" ? theme : String(theme);
  const dark = name === "dark";
  document.body.classList.toggle("dark", dark);
  document.body.classList.toggle("light", !dark);
  // swap the highlight.js theme with the IDE theme
  const lightCss = document.getElementById("hljs-light");
  const darkCss = document.getElementById("hljs-dark");
  if (lightCss && darkCss) {
    lightCss.disabled = dark;
    darkCss.disabled = !dark;
  }
  mermaidReady = false; // re-init lazily with the right theme
  report("theme set: " + name);
  return true;
});

window.__clear = guard("__clear", function () {
  chatEl.innerHTML = "";
  return true;
});

window.__setNotice = guard("__setNotice", function (text) {
  const div = document.createElement("div");
  div.className = "notice";
  div.textContent = typeof text === "string" ? text : String(text);
  chatEl.appendChild(div);
  scrollBottom();
  report("notice rendered: " + div.textContent.slice(0, 60));
  return true;
});

window.__setMessages = guard("__setMessages", function (json) {
  chatEl.innerHTML = "";
  const entries = payload(json);
  entries.forEach(e => {
    if (e.role === "user") {
      addUser(e.text);
    } else {
      const a = addAssistant(e.id, e.reasoning);
      renderMarkdown(a.body, e.text);
      if (e.meta) { const m = document.createElement("div"); m.className = "meta"; m.textContent = e.meta; a.bubble.appendChild(m); }
    }
  });
  scrollBottom();
  report("history rendered (" + entries.length + " entries)");
  return true;
});

window.__appendUser = guard("__appendUser", function (json) {
  const p = payload(json);
  addUser(typeof p.text === "string" ? p.text : "");
  return true;
});

window.__startAssistant = guard("__startAssistant", function (json) {
  const p = payload(json);
  if (!findAssistant(p.mid)) addAssistant(p.mid, null);
  return true;
});

window.__appendDelta = guard("__appendDelta", function (json) {
  const p = payload(json);
  let node = findAssistant(p.mid);
  if (!node) { addAssistant(p.mid, null); node = findAssistant(p.mid); }
  if (!node) return true;
  let body = node.querySelector(".body");
  let raw = body.querySelector(".stream-raw");
  if (!raw) { body.innerHTML = ""; raw = document.createElement("div"); raw.className = "stream-raw"; body.appendChild(raw); }
  raw.appendChild(document.createTextNode(p.text == null ? "" : String(p.text)));
  let cursor = raw.querySelector(".cursor");
  if (!cursor) { cursor = document.createElement("span"); cursor.className = "cursor"; }
  raw.appendChild(cursor);
  scrollBottom();
  return true;
});

window.__setAssistantText = guard("__setAssistantText", function (json) {
  const p = payload(json);
  const text = typeof p.text === "string" ? p.text : "";
  let node = findAssistant(p.mid);
  if (!node) { addAssistant(p.mid, p.reasoning || null); node = findAssistant(p.mid); }
  if (!node) return true;
  const body = node.querySelector(".body");
  body.innerHTML = "";
  renderMarkdown(body, text);
  if (p.meta) { let m = node.querySelector(".meta"); if (!m) { m = document.createElement("div"); m.className = "meta"; node.querySelector(".bubble").appendChild(m); } m.textContent = p.meta; }
  scrollBottom();
  report("assistant bubble rendered (" + text.length + " chars, meta=" + (p.meta || "") + ")");
  return true;
});

// ---- links -----------------------------------------------------------------
// Links must never navigate this view: the chat page would be replaced by the
// target site and the whole transcript would be gone. Every link is handed to
// Eclipse, which opens it in the external browser.
function linkTargetOf(node) {
  while (node && node !== chatEl) {
    if (node.tagName === "A") return node;
    node = node.parentElement;
  }
  return null;
}

// exposed for the automated bridge test (same function the listener uses)
window.__linkClick = function (event) {
  const anchor = linkTargetOf(event && event.target);
  if (!anchor) return false;
  const href = anchor.getAttribute ? anchor.getAttribute("href") : null;
  if (!href || href.charAt(0) === "#") return false;
  if (event.preventDefault) event.preventDefault();
  if (typeof window.__javaOpenExternal === "function") {
    window.__javaOpenExternal(href);
  } else {
    report("external link (no Java bridge): " + href);
  }
  return true;
};
chatEl.addEventListener("click", window.__linkClick);

// The page announces readiness to Java (authoritative signal - flushes queued renders).
report("page-ready");
