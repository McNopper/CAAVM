// bridge-check.mjs - executes the chat page's Java bridge against a DOM shim.
//
// This is the test that the old string-matching renderer-check could not do:
// it runs chat.html's script and calls the bridge with the EXACT script strings
// Java produces (com.opencode.ide.chat.internal.ChatScripts), then asserts that
// content actually lands in the DOM. The original bug (Java passed a JS object
// literal while the page did JSON.parse(string), so every render threw and
// Browser.execute still returned true) fails loudly here.
//
// Run: node bridge-check.mjs   (exit 0 = pass)
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { createRequire } from "node:module";
import vm from "node:vm";

const require = createRequire(import.meta.url);
// WEB_DIR override exists so the suite can be run against a mutated copy of the
// page (used to prove these checks actually fail when the bridge is broken).
const webDir = process.env.WEB_DIR
  || join(dirname(fileURLToPath(import.meta.url)), "web");
let failures = 0;
const check = (name, ok, detail = "") => {
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? " - " + detail : ""}`);
  if (!ok) failures++;
};

// ---------------- minimal DOM shim ----------------

const stripTags = (html) => String(html).replace(/<[^>]*>/g, "");
/** Visible text of rendered HTML: tags removed and entities decoded. */
const visibleText = (html) => stripTags(html)
  .replace(/&lt;/g, "<").replace(/&gt;/g, ">")
  .replace(/&quot;/g, "\"").replace(/&#x27;|&apos;/g, "'")
  .replace(/&amp;/g, "&");

class El {
  constructor(tag) {
    this.tagName = String(tag).toUpperCase();
    this.children = [];
    this.dataset = {};
    this.className = "";
    this.parentElement = null;
    this.scrollTop = 0;
    this.scrollHeight = 0;
    this._html = null;
    this._text = null;
    this.nodeType = 1;
  }
  get classList() {
    const self = this;
    const list = () => self.className.split(/\s+/).filter(Boolean);
    return {
      contains: (c) => list().includes(c),
      add: (c) => { if (!list().includes(c)) self.className = list().concat(c).join(" "); },
      remove: (c) => { self.className = list().filter(x => x !== c).join(" "); },
      toggle: (c, force) => {
        const on = force === undefined ? !list().includes(c) : !!force;
        if (on) { if (!list().includes(c)) self.className = list().concat(c).join(" "); }
        else { self.className = list().filter(x => x !== c).join(" "); }
        return on;
      }
    };
  }
  appendChild(child) {
    child.parentElement = this;
    this.children.push(child);
    return child;
  }
  addEventListener(type, handler) {
    (this._listeners || (this._listeners = {}))[type] = handler;
  }
  set innerHTML(html) { this._html = String(html); this._text = null; this.children = []; }
  get innerHTML() { return this._html === null ? "" : this._html; }
  set textContent(text) { this._text = String(text); this._html = null; this.children = []; }
  get textContent() { return textOf(this); }
  replaceWith(node) {
    const parent = this.parentElement;
    if (!parent) return;
    parent.children[parent.children.indexOf(this)] = node;
    node.parentElement = parent;
  }
  querySelector(sel) { return this.querySelectorAll(sel)[0] || null; }
  querySelectorAll(sel) {
    const parts = String(sel).split(">").map(s => s.trim());
    const target = parts[parts.length - 1];
    const parentSel = parts.length > 1 ? parts[parts.length - 2] : null;
    const out = [];
    walk(this, (el) => {
      if (matches(el, target) && (!parentSel || (el.parentElement && matches(el.parentElement, parentSel)))) {
        out.push(el);
      }
    });
    return out;
  }
}

function walk(el, visit) {
  for (const child of el.children) {
    if (child.nodeType === 1) { visit(child); walk(child, visit); }
  }
}

// supports: tag, .class(.class), [attr="value"] (data-* only)
function matches(el, selector) {
  const attr = selector.match(/\[data-([a-zA-Z0-9_-]+)="([^"]*)"\]/);
  let rest = selector.replace(/\[[^\]]*\]/g, "");
  if (attr && el.dataset[attr[1]] !== attr[2]) return false;
  const classes = (rest.match(/\.[A-Za-z0-9_-]+/g) || []).map(c => c.slice(1));
  const tag = rest.replace(/\.[A-Za-z0-9_-]+/g, "").trim();
  if (tag && el.tagName !== tag.toUpperCase()) return false;
  const own = el.className.split(/\s+/).filter(Boolean);
  return classes.every(c => own.includes(c));
}

function textOf(node) {
  if (node.nodeType === 3) return node.textContent;
  let text = node._text !== null && node._text !== undefined ? node._text
    : (node._html ? stripTags(node._html) : "");
  for (const child of node.children) text += textOf(child);
  return text;
}

// ---------------- load the page script ----------------

const html = readFileSync(join(webDir, "chat.html"), "utf8");
check("chat.html loads the app script", html.includes("chat.js"));
const script = readFileSync(join(webDir, "chat.js"), "utf8");
check("chat.js is the real app script", script.includes("__appendUser") && script.includes("extractMath"));

const chatEl = new El("div");
const bodyEl = new El("body");
const reports = [];
const ctx = {
  console,
  JSON, Math, Date, String, Number, Boolean, Object, Array, RegExp, Error, parseInt, parseFloat,
  setTimeout, clearTimeout,
  markdownit: require(join(webDir, "markdown-it.min.js")),
  document: {
    body: bodyEl,
    getElementById: (id) => (id === "chat" ? chatEl : null),
    createElement: (tag) => new El(tag),
    createTextNode: (text) => ({ nodeType: 3, textContent: String(text), children: [] })
  },
  addEventListener: () => {},
  __javaReport: (msg) => reports.push(String(msg))
};
ctx.katex = require(join(webDir, "katex", "katex.min.js"));
ctx.hljs = require(join(webDir, "hljs", "highlight.min.js"));
// language packs self-register against the hljs instance (as in the browser)
globalThis.hljs = ctx.hljs; // the UMD language pack registers against the global, as in the browser
try {
  const cmake = require(join(webDir, "hljs", "cmake.min.js"));
  if (typeof cmake === "function") ctx.hljs.registerLanguage("cmake", cmake);
} catch (e) {
  console.log("NOTE  cmake language pack not registered in Node: " + e.message);
}
ctx.window = ctx;
ctx.globalThis = ctx;
vm.createContext(ctx);

let loadError = null;
try {
  vm.runInContext(script, ctx, { filename: "chat.html" });
} catch (e) {
  loadError = e;
}
check("page script runs without throwing", loadError === null, loadError ? String(loadError) : "");
check("page reports page-ready", reports.includes("page-ready"));

// Runs a script string exactly as SWT Browser.execute() would.
const exec = (js) => vm.runInContext(js, ctx, { filename: "bridge" });

// ---------------- the Java -> JS contract (ChatScripts output) ----------------

// These literals are what com.opencode.ide.chat.internal.ChatScripts produces;
// ChatScriptsTest asserts the Java side emits exactly this shape.
const r1 = exec('window.__appendUser("{\\"text\\":\\"hello **world**\\"}")');
check("__appendUser(JSON string) returns true", r1 === true);
check("__appendUser renders the prompt text", textOf(chatEl).includes("hello world"),
  JSON.stringify(textOf(chatEl)).slice(0, 80));
check("__appendUser reports to Java", reports.some(r => r.startsWith("user bubble rendered")));

// object-literal form must not silently do nothing (the original bug)
const before = reports.length;
const r2 = exec('window.__appendUser({"text":"object form"})');
check("__appendUser(object) is tolerated", r2 === true && textOf(chatEl).includes("object form"));
check("__appendUser(object) reported a render", reports.length > before);

// streaming
exec('window.__startAssistant("{\\"mid\\":\\"msg_1\\"}")');
exec('window.__appendDelta("{\\"mid\\":\\"msg_1\\",\\"text\\":\\"ack\\"}")');
exec('window.__appendDelta("{\\"mid\\":\\"msg_1\\",\\"text\\":\\"nowledged\\"}")');
const streamNode = chatEl.querySelector('.msg.assistant[data-mid="msg_1"]');
check("__startAssistant creates the reply bubble", !!streamNode);
check("__appendDelta streams text into it", !!streamNode && textOf(streamNode).includes("acknowledged"),
  streamNode ? JSON.stringify(textOf(streamNode)) : "no node");

// final authoritative render (markdown + meta)
const finalScript = 'window.__setAssistantText("{\\"mid\\":\\"msg_1\\",\\"text\\":\\"# Done\\\\n\\\\n`code` and $x^2$\\",'
  + '\\"reasoning\\":\\"\\",\\"meta\\":\\"anthropic/claude\\"}")';
const r3 = exec(finalScript);
check("__setAssistantText returns true", r3 === true);
check("__setAssistantText renders markdown", !!streamNode && streamNode.querySelector(".body").innerHTML.includes("<h1>Done</h1>"));
check("__setAssistantText shows the model meta", !!streamNode && textOf(streamNode).includes("anthropic/claude"));
check("__setAssistantText reports to Java", reports.some(r => r.startsWith("assistant bubble rendered")));

// history load (resume)
const rows = JSON.stringify([
  { role: "user", id: "", text: "prior question", reasoning: "", meta: "" },
  { role: "assistant", id: "msg_0", text: "prior **answer**", reasoning: "", meta: "openai/gpt" }
]);
const r4 = exec("window.__setMessages(" + JSON.stringify(rows) + ")");
check("__setMessages returns true", r4 === true);
check("__setMessages renders history", textOf(chatEl).includes("prior question") && textOf(chatEl).includes("prior answer"));
check("__setMessages replaced the transcript", !textOf(chatEl).includes("hello world"));

// notice + theme (plain string args)
exec('window.__setNotice("Connected.")');
check("__setNotice renders", textOf(chatEl).includes("Connected."));
exec('window.__setTheme("dark")');
check("__setTheme applies the dark class", bodyEl.className.includes("dark"));
exec('window.__setTheme("light")');
check("__setTheme switches back to light", bodyEl.className.includes("light") && !bodyEl.className.includes("dark"));

// clear
exec("window.__clear()");
check("__clear empties the transcript", textOf(chatEl) === "");

// ---------------- math (extract BEFORE markdown) ----------------
// Real model output captured from a live opencode server, plus the shapes that
// the previous "markdown first, KaTeX auto-render after" pipeline destroyed.
const mathCase = (name, markdown, expectations) => {
  exec("window.__clear()");
  exec("window.__setAssistantText(" + JSON.stringify(JSON.stringify(
    { mid: "m_math", text: markdown, reasoning: "", meta: "" })) + ")");
  const node = chatEl.querySelector('.msg.assistant[data-mid="m_math"]');
  const html = node ? node.querySelector(".body").innerHTML : "";
  expectations(name, html);
};

mathCase("real model output ($$ on one line)",
  "Quadratic formula:\n$$x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$$\n\nThe roots $x_1$ and $x_2$ satisfy $ax^2 + bx + c = 0$.",
  (name, html) => {
    check(name + ": display math rendered by KaTeX", html.includes("katex-display"));
    check(name + ": inline math rendered by KaTeX", html.includes("class=\"katex\""));
    check(name + ": no raw $$ left in the output", !html.includes("$$"));
  });

mathCase("multi-line $$ block", "$$\nx = \\frac{-b}{2a}\n$$\n", (name, html) => {
  check(name + ": rendered (used to be raw $$ + <br>)", html.includes("katex-display"));
  check(name + ": no stray <br> inside the formula", !/\$\$/.test(html));
});

mathCase("\\( \\) and \\[ \\] delimiters", "inline \\(x^2\\) and display \\[y = 1\\]", (name, html) => {
  check(name + ": both rendered (markdown used to eat the backslashes)",
    (html.match(/class="katex/g) || []).length >= 2);
  check(name + ": no leftover parenthesis delimiters", !html.includes("\\(") && !html.includes("(x^2)"));
});

mathCase("LaTeX line break \\\\ inside math", "$$a \\\\ b$$", (name, html) => {
  check(name + ": survived markdown (\\\\ used to collapse to \\)", html.includes("katex"));
});

mathCase("math must not touch code blocks", "```c\nint cost = $5; /* $x$ */\n```", (name, html) => {
  check(name + ": no KaTeX inside code", !html.includes("katex"));
  // hljs tokenises, so compare the text content rather than the raw markup
  check(name + ": code content intact", stripTags(html).includes("$5") && stripTags(html).includes("$x$"),
    JSON.stringify(stripTags(html)));
});

mathCase("currency is not math", "It costs $5 and then $10 more.", (name, html) => {
  check(name + ": left as plain text", !html.includes("katex") && html.includes("$5"));
});

mathCase("broken TeX degrades visibly, never silently", "$$\\frac{1}{$$", (name, html) => {
  check(name + ": something is rendered", html.length > 0);
});

// ---------------- syntax highlighting ----------------
const codeCase = (name, markdown, expectations) => {
  exec("window.__clear()");
  exec("window.__setAssistantText(" + JSON.stringify(JSON.stringify(
    { mid: "m_code", text: markdown, reasoning: "", meta: "" })) + ")");
  const node = chatEl.querySelector('.msg.assistant[data-mid="m_code"]');
  expectations(name, node ? node.querySelector(".body").innerHTML : "");
};

codeCase("C code block", "```c\n#include <stdio.h>\nint main(void) { return 0; }\n```", (name, html) => {
  check(name + ": highlighted", html.includes("hljs-"));
  check(name + ": tagged as C", html.includes("language-c"));
  check(name + ": preprocessor token recognised", html.includes("hljs-meta"));
  check(name + ": angle brackets escaped", html.includes("&lt;stdio.h&gt;"));
});

codeCase("C++ code block", "```cpp\ntemplate<typename T>\nclass Foo { public: T bar() const noexcept; };\n```",
  (name, html) => {
    check(name + ": highlighted", html.includes("hljs-keyword"));
    check(name + ": tagged as C++", html.includes("language-cpp"));
  });

codeCase("CMake code block", "```cmake\nadd_executable(app main.c)\n```", (name, html) => {
  check(name + ": cmake language registered", html.includes("language-cmake"));
});

codeCase("unknown language does not break", "```klingon\nqapla'\n```", (name, html) => {
  check(name + ": still rendered as code", html.includes("<code") && html.includes("qapla"));
});

codeCase("untagged fence still styled", "```\nplain text\n```", (name, html) => {
  check(name + ": hljs class present", html.includes("hljs"));
});

codeCase("mermaid fence is NOT highlighted", "```mermaid\ngraph TD; A-->B;\n```", (name, html) => {
  check(name + ": kept as language-mermaid for the diagram pass", html.includes("language-mermaid"));
  check(name + ": not tokenised by hljs", !html.includes("hljs-keyword"));
});

codeCase("code cannot inject HTML", "```html\n<script>alert(1)</script>\n```", (name, html) => {
  // hljs emits &lt;<span class="hljs-name">script</span>&gt; - what matters is that
  // no executable tag survives and the text is still readable
  check(name + ": no executable tag in the output", !/<script/i.test(html));
  check(name + ": shown as escaped text", visibleText(html).includes("<script>alert(1)</script>"),
    JSON.stringify(visibleText(html)));
});

// ---------------- external links ----------------
exec("window.__clear()");
const opened = [];
ctx.__javaOpenExternal = (url) => opened.push(url);
let prevented = false;
const anchor = { tagName: "A", parentElement: null, getAttribute: () => "https://opencode.ai/docs" };
const handled = ctx.__linkClick({ target: anchor, preventDefault: () => { prevented = true; } });
check("link click is intercepted", handled === true);
check("link click is not followed in the view", prevented === true);
check("link is handed to Eclipse for the external browser",
  opened.length === 1 && opened[0] === "https://opencode.ai/docs");

const anchorHash = { tagName: "A", parentElement: null, getAttribute: () => "#section" };
const handledHash = ctx.__linkClick({ target: anchorHash, preventDefault: () => {} });
check("in-page anchors are left alone", handledHash === false && opened.length === 1);

const notALink = { tagName: "SPAN", parentElement: null, getAttribute: () => null };
check("clicks on non-links are ignored",
  ctx.__linkClick({ target: notALink, preventDefault: () => {} }) === false);

// ---------------- failures must be loud ----------------
const errBefore = reports.length;
const bad = exec('window.__appendUser("not json at all")');
check("malformed payload returns false", bad === false);
check("malformed payload is reported as a JS ERROR",
  reports.slice(errBefore).some(r => r.startsWith("JS ERROR in __appendUser")),
  reports.slice(errBefore).join(" | "));

process.exit(failures === 0 ? 0 : 1);
