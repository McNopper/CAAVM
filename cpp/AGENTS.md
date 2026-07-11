# AGENTS.md — C++23 AI-first template

Canonical commands and conventions for AI agents working in this template.
Run everything from this directory (the one containing `CMakeLists.txt`).

## Toolchain

- **Recommended:** Ninja generator + a Clang- or GNU-compatible compiler.
  This unlocks the full static-analysis stack (clang-tidy, cppcheck) which is
  driven by the exported `compile_commands.json`.
- **Any other toolchain still builds.** With MSVC or a non-compile-database
  generator the project configures and builds normally; the Clang-based
  analysis is simply skipped (CMake prints a status line saying so). Do not
  treat skipped analysis as an error.

## Canonical commands

| Goal | Command |
|------|---------|
| Configure (Debug + analysis) | `cmake --preset default` |
| Configure (Release)          | `cmake --preset release` |
| Build (Debug)                | `cmake --build build` |
| Build (Release)              | `cmake --build build-release` |
| Run tests                    | `ctest --preset default` |
| **Verify (fast default)**    | `cmake --build build --target verify` |
| **Verify (full strict)**     | `cmake --build build --target verify-full` |
| Format code (in place)       | `cmake --build build --target format` |
| Check formatting             | `cmake --build build --target format-check` |
| clang-tidy + fixes report    | `cmake --build build --target tidy` |
| cppcheck (default profile)   | `cmake --build build --target cppcheck` |
| cppcheck (exhaustive)        | `cmake --build build --target cppcheck-strict` |
| cppcheck XML report          | `cmake --build build --target cppcheck-xml` |
| Generate docs (HTML+XML)     | `cmake --build build --target docs` |

`verify` is intentionally fast (build + tests). It always prints whether static
analysis is enabled or skipped for the current toolchain and writes a status
artifact. A green `verify` with analysis skipped is still useful, but it is a
degraded signal compared to full analysis.

Use `verify-full` for strict validation (verify + format-check + static
analysis + docs).

## MCP server (optional)

The [`mcp/`](mcp/) subtree exposes the same tooling to opencode agents as
structured MCP tools (`cpp_configure`, `cpp_build`, `cpp_verify`, `cpp_docs`,
`cpp_analysis_status`, `cpp_read_report`) that return JSON instead of log text.
It also provides standalone `cpp_cppcheck` / `cpp_format` / `cpp_clang_tidy`
tools that run on **any** C++ project with caller-supplied settings. Prefer the
MCP tools when the server is enabled; fall back to the raw CMake targets
otherwise. See [`mcp/README.md`](mcp/README.md) for install and wiring.

## Machine-readable outputs

All under `${binaryDir}` for the selected configure preset (default
`${sourceDir}/build`):

| Artifact | Path | Consumer |
|----------|------|----------|
| Analysis status       | `${binaryDir}/reports/analysis-status.txt` | enabled/skipped contract |
| Compile database      | `${binaryDir}/compile_commands.json`        | clang-tidy, cppcheck, editors |
| clang-tidy fixes      | `${binaryDir}/reports/clang-tidy/fixes.yaml` | parse/apply suggested edits |
| cppcheck XML          | `${binaryDir}/reports/cppcheck/cppcheck.xml` | parse findings |
| Doxygen XML           | `${binaryDir}/docs/xml/`                     | symbol/structure indexing |
| Doxygen tagfile       | `${binaryDir}/docs/MyProject.tag`            | compact symbol cross-reference |
| Doxygen warnings      | `${binaryDir}/docs/doxygen_warnings.log`     | undocumented/broken-link signal |

## Conventions

- C++23, no compiler extensions.
- Layout: public headers in `include/`, implementation in `src/`, tests in
  `tests/` (GoogleTest, fetched automatically).
- `.clang-tidy` only **errors** (`WarningsAsErrors`) on the core correctness
  checks: clang-diagnostic, clang-analyzer, and bugprone. Other enabled checks
  (performance, portability, modernize) run as **warnings** — guidance, not gates.
  Subjective/style checks (magic numbers, identifier length, brace and
  function-size mandates) are intentionally off so they do not block code generation.
- `tests/` are excluded from cppcheck (GoogleTest macros confuse its parser);
  they are still covered by clang-tidy.
- Formatting is defined by `.clang-format`; run the `format` target rather than
  hand-aligning code.

## Guardrails

- `verify` must always expose whether analysis is `enabled` or `skipped`.
- Treat green `verify` with skipped analysis as degraded, not equivalent to full signal.
- Keep blocking checks focused on correctness/security classes.
- Do not add subjective style checks as hard gates.
- Keep machine-readable artifacts in stable `${binaryDir}` paths.
- Keep third-party diagnostics from drowning first-party findings.
- Add strictness with opt-in targets (for example `*-strict`, `verify-full`), not by increasing default friction.

## Feature toggles (CMake options)

`ENABLE_CLANG_TIDY`, `ENABLE_CLANG_TIDY_IN_BUILD`, `ENABLE_CLANG_FORMAT`,
`ENABLE_CPPCHECK`, `ENABLE_CPPCHECK_IN_BUILD`, `ENABLE_TESTING`,
`ENABLE_DOXYGEN` — all default `ON` except `ENABLE_CPPCHECK_IN_BUILD`.
Pass e.g. `-DENABLE_DOXYGEN=OFF` at configure time.
