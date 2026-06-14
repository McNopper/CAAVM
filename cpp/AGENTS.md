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
| Build                        | `cmake --build build` |
| Run tests                    | `ctest --preset default` |
| **Verify everything**        | `cmake --build build --target verify` |
| Format code (in place)       | `cmake --build build --target format` |
| Check formatting             | `cmake --build build --target format-check` |
| clang-tidy + fixes report    | `cmake --build build --target tidy` |
| cppcheck (default profile)   | `cmake --build build --target cppcheck` |
| cppcheck (exhaustive)        | `cmake --build build --target cppcheck-strict` |
| cppcheck XML report          | `cmake --build build --target cppcheck-xml` |
| Generate docs (HTML+XML)     | `cmake --build build --target docs` |

`verify` is the single command an agent should run before declaring work done:
it builds the app and tests, runs the test suite, checks formatting, and runs
the default cppcheck profile.

## Machine-readable outputs

All under the build directory (default `build/`):

| Artifact | Path | Consumer |
|----------|------|----------|
| Compile database     | `build/compile_commands.json`        | clang-tidy, cppcheck, editors |
| clang-tidy fixes      | `build/reports/clang-tidy/fixes.yaml` | parse/apply suggested edits |
| cppcheck XML          | `build/reports/cppcheck/cppcheck.xml` | parse findings |
| Doxygen XML           | `build/docs/xml/`                     | symbol/structure indexing |
| Doxygen tagfile       | `build/docs/MyProject.tag`            | compact symbol cross-reference |
| Doxygen warnings      | `build/docs/doxygen_warnings.log`     | undocumented/broken-link signal |

## Conventions

- C++23, no compiler extensions.
- Layout: public headers in `include/`, implementation in `src/`, tests in
  `tests/` (GoogleTest, fetched automatically).
- `.clang-tidy` only **errors** on correctness checks (clang-diagnostic,
  clang-analyzer, bugprone, performance, portability). Modernize checks are
  warnings — guidance, not gates. Subjective/style checks (magic numbers,
  identifier length, brace and function-size mandates) are intentionally off
  so they do not block code generation.
- `tests/` are excluded from cppcheck (GoogleTest macros confuse its parser);
  they are still covered by clang-tidy.
- Formatting is defined by `.clang-format`; run the `format` target rather than
  hand-aligning code.

## Feature toggles (CMake options)

`ENABLE_CLANG_TIDY`, `ENABLE_CLANG_TIDY_IN_BUILD`, `ENABLE_CLANG_FORMAT`,
`ENABLE_CPPCHECK`, `ENABLE_CPPCHECK_IN_BUILD`, `ENABLE_TESTING`,
`ENABLE_DOXYGEN` — all default `ON` except `ENABLE_CPPCHECK_IN_BUILD`.
Pass e.g. `-DENABLE_DOXYGEN=OFF` at configure time.
