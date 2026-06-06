# INPUT — your ideas & requirements (freeform, human-only)

> **This file is yours.** Write plain sentences here — at any time, even while the
> process is running. **The process only ever READS this file; it never edits it.**
> Everything it understands, derives, creates, or updates is reported back in
> [`OUTPUT.md`](OUTPUT.md) (which is the process's file — you never edit that one).
>
> At the start of every cycle the Hephaestus *Intake* step reads this file and:
> - maps high-level choices (language, design, tools, gates, docs level) into
>   [`config/hephaestus.config.yaml`](config/hephaestus.config.yaml);
> - turns feature/requirement ideas into backlog increments.
>
> **Minimal is fine.** The config already holds sensible defaults (C++23, CMake+Ninja,
> clang-format/clang-tidy/cppcheck, GoogleTest, hexagonal design, …), so you only write
> what should *differ*. A single line is a valid INPUT — e.g.:
>
> > _"A small C++23 CLI that converts a CSV file to JSON."_
>
> Leave any section below blank when the default is good enough.

---

## 🧭 Project setup (plain sentences — the process turns these into config)

> Examples of sentences that get mapped: _"Use C++23."_ · _"Build with CMake + Ninja."_ ·
> _"Use a hexagonal (ports & adapters) design."_ · _"Lint with clang-tidy, clang-format,
> cppcheck."_ · _"Test with GoogleTest."_ · _"Require 85% line coverage."_ ·
> _"Keep documentation minimal."_ · _"Use Opus for architecture/design, Haiku for verification."_

- 

---

## ✨ Features / requirements (what the software should do)

> One idea per bullet is easiest, but free prose is fine. Each becomes a backlog
> increment (one vertical V-pass).

- 

---

> 👀 Want to see what the process understood and did with the above? It's all in
> [`OUTPUT.md`](OUTPUT.md) — this file stays exactly as you wrote it.
