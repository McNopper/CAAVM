# INPUT — your ideas & requirements (freeform)

> **This is your half of the interface.** Write plain sentences here — at any time,
> even while the process is running. At the start of every cycle the CAAVM *Intake*
> step reads this file and **updates the project files to match**:
>
> - high-level choices (language, design, tools, gates, docs level) → rewritten into
>   [`config/caav-model.config.yaml`](config/caav-model.config.yaml);
> - feature/requirement ideas → turned into backlog increments;
> - what it captured is logged in the table at the bottom (your prose is never deleted).
>
> The companion file [`OUTPUT.md`](OUTPUT.md) reports the current state back to you.
>
> **Minimal is fine.** The config files already hold sensible defaults (C++23, CMake+Ninja,
> clang-format/clang-tidy/cppcheck, GoogleTest, hexagonal design, …), so you only write what
> should *differ*. A single line is a valid INPUT — e.g.:
>
> > _"A small C++23 CLI that converts a CSV file to JSON."_
>
> Everything you don't mention is taken from the defaults; leave the sections below blank
> when the default is good enough.

---

## 🧭 Project setup (plain sentences — the process turns these into config)

> Just write it however you like. Examples of the kinds of sentences that get mapped:
> _"Use C++23."_ · _"Build with CMake + Ninja."_ · _"Use a hexagonal (ports & adapters)
> design."_ · _"Lint with clang-tidy and clang-format, plus cppcheck."_ · _"Test with
> GoogleTest."_ · _"Require 85% line coverage."_ · _"Keep documentation minimal."_ ·
> _"Use Opus for architecture and design, Haiku for verification."_

- 

---

## ✨ Features / requirements (what the software should do)

> One idea per bullet is easiest, but free prose is fine. Each becomes a backlog
> increment (one vertical V-pass).

- 

---

## 📥 Captured (maintained by the process — leave this for the agent)

> Intake appends a row here for everything it ingested — config changes and backlog
> items — so you can see what was understood.

| Input (short) | Mapped to | Captured |
|---------------|-----------|----------|
| _nothing captured yet_ | — | — |
