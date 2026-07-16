# AI-first C++23 build template

## About this document
- **Kind:** `doc` / C++ subtree README
- **Read by:** humans adopting the C++ skeleton; **written by:** maintainers
- **Related:** pairs with `cpp/AGENTS.md` (command manifest)

A lean, modern C++23 project skeleton whose tooling is configured to emit
**structured, machine-readable information** that an AI agent can use to
navigate, refactor, and verify the code — while staying out of the way when
code is being generated.

The guiding idea: the static analysis is here so an agent can **detect real
problems fast in otherwise well-written code**, not to punish generation with
subjective style gates.

## Layout

```
cpp/
├── CMakeLists.txt          # build + analysis wiring
├── CMakePresets.json       # default / release / analysis presets (Ninja)
├── AGENTS.md               # canonical command manifest for agents
├── .clang-format           # formatting rules
├── .clang-tidy             # high-signal, low-friction check set
├── .gitignore              # ignores build*/ trees
├── cppcheck.supp           # cppcheck suppressions
├── Doxyfile.in             # AI-oriented Doxygen config (XML + tagfile)
├── cmake/
│   └── cppcheck.cmake      # single source of truth for cppcheck targets
├── include/
│   └── example.hpp
├── src/
│   ├── example.cpp
│   └── main.cpp
└── tests/
    └── example_test.cpp
```

## Quick start

```bash
cmake --preset default                       # configure (Debug + analysis)
cmake --build build --target verify          # fast: build + test + analysis status
cmake --build build --target verify-full     # full: verify + format + static analysis + docs
```

See **[AGENTS.md](AGENTS.md)** for the full command manifest and the locations
of every machine-readable report.

## C++ execution (cpp-tools agent)

C++ work in this repo is driven by the `cpp-tools` **agent** (methodology in the
`cpp-tools` skill). It runs the CMake targets via bash and reads the machine-readable
reports. There is no separate MCP server — C++ is an agent now.

## Toolchain

- **Recommended:** Ninja + a Clang- or GNU-compatible compiler. This exports
  `compile_commands.json` and enables the full clang-tidy / cppcheck stack.
- **Other toolchains still build.** With MSVC, or a generator that has no
  compile database, the project configures and builds normally and the
  Clang-based analysis is skipped (CMake says so at configure time). This is by
  design, expressed as a positive allowlist rather than blocking any specific
  compiler.

**External tools must be on PATH:** [CMake](https://cmake.org/download/) (≥ 3.26),
[Ninja](https://ninja-build.org/), [cppcheck](https://github.com/danmar/cppcheck), and
the LLVM/Clang tools (`clang-format`, `clang-tidy`).

## What makes it "AI-first"

- **`compile_commands.json`** exported for every analysis tool and editor.
- **Doxygen XML + tagfile** (`build/docs/xml/`, `build/docs/MyProject.tag`) —
  machine-readable structure and a compact symbol index, alongside HTML.
- **clang-tidy fix export** (`build/reports/clang-tidy/fixes.yaml`) — findings
  *and* suggested edits an agent can apply.
- **cppcheck XML** (`build/reports/cppcheck/cppcheck.xml`).
- **analysis status contract** (`build/reports/analysis-status.txt`) with
  `analysis=enabled|skipped` and toolchain reason.
- Stable, predictable report paths under `${binaryDir}/reports/`.
- Two verification levels: fast **`verify`** and strict **`verify-full`**.

## Analysis philosophy

`.clang-tidy` errors only on correctness-oriented checks
(`clang-diagnostic`, `clang-analyzer`, `bugprone`). `performance`,
`portability`, and modernization checks stay warnings. Friction-causing checks are
deliberately disabled: magic numbers, identifier length, brace/function-size
mandates, swappable parameters, `bugprone-exception-escape` (a known platform
false positive), and `portability-avoid-pragma-once`.

cppcheck runs a high signal-to-noise default profile
(`warning,performance,portability`); an exhaustive `cppcheck-strict` profile
(`all + inconclusive`) is available opt-in.

## Renaming the project

Replace `MyProject` / `my_project_*` in `CMakeLists.txt` and the `example`
sources with your own names; everything else adapts automatically.
