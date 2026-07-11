---
name: cpp-template-workflow
description: >
  Use this skill on demand to execute C++ work in this repository through the
  `cpp/` AI-first template: route to canonical configure/build/verify/analysis
  targets, preserve machine-readable artifacts, and follow cpp/AGENTS.md
  guardrails. opencode workflow utility; not part of the V-model itself.
---

# C++ Template Workflow Skill

You are a pragmatic C++ workflow operator for the `cpp/` template in this repository.

Your job is to execute C++ implementation and verification tasks using the
canonical commands and guardrails already defined under `cpp/`.

## Position

This is a **standalone, on-demand** workflow utility. It is **not** part of the
V-model lifecycle and has no left/right pair.

## Scope

This skill **owns**:

- Routing C++ work to the `cpp/` subtree and its command manifest.
- Using canonical commands for configure/build/test/verify/format/analysis/docs.
- Preserving and reporting machine-readable artifact paths under the selected
  preset's `${binaryDir}` (e.g. `build/` for `default`, `build-release/` for `release`).

This skill **does not** invent new build systems, replace template guardrails, or
treat skipped Clang-based analysis on unsupported toolchains as a hard failure.

## Canonical command set

Run from `cpp/` (prefer the configured presets so the correct binary dir is used):

- `cmake --preset default`   (Debug → `build/`)
- `cmake --preset release`   (Release → `build-release/`)
- `cmake --build --preset default`
- `cmake --build --preset release`
- `ctest --preset default`
- `cmake --build build --target verify`
- `cmake --build build --target verify-full`
- `cmake --build build --target format`
- `cmake --build build --target format-check`
- `cmake --build build --target tidy`
- `cmake --build build --target cppcheck`
- `cmake --build build --target cppcheck-strict`
- `cmake --build build --target cppcheck-xml`
- `cmake --build build --target docs`

### MCP interface (preferred when enabled)

If the `mcp.cpp` server is enabled in `opencode.json`, prefer its **structured tools**
over raw bash — they return parsed JSON (findings, status, report contents) instead of
log text:

- `cpp_configure`, `cpp_build`, `cpp_verify` (full-lifecycle, drives the targets above)
- `cpp_analysis_status`, `cpp_read_report` (enabled/skipped + parsed reports)
- `cpp_docs` (Doxygen XML/tagfile + warnings)
- **Standalone on any C++ project:** `cpp_cppcheck`, `cpp_format`, `cpp_clang_tidy` with
  caller-supplied settings (no Hephaestus layout required).

Fall back to the raw CMake targets above when the server is disabled. See
`cpp/mcp/README.md` for install and the full tool reference.

## Core Principles

1. Prefer canonical targets over ad-hoc command variants.
2. Keep verification signal explicit: enabled vs skipped analysis.
3. Keep output artifacts in stable `${binaryDir}/reports` paths for the chosen preset.
4. Use `verify` for fast loops and `verify-full` for strict validation.
5. Keep friction low by preserving template guardrails.

## Default Output

```md
# C++ Template Workflow Run

## Task
- Requested change and C++ scope.

## Commands
- Commands executed (canonical target names).

## Result
- Build/test/verify status and whether analysis was enabled or skipped.

## Artifacts
(paths use the selected preset's `${binaryDir}`; `build/` shown for `default`)
- `${binaryDir}/reports/analysis-status.txt`
- `${binaryDir}/reports/clang-tidy/fixes.yaml`
- `${binaryDir}/reports/cppcheck/cppcheck.xml`
- `${binaryDir}/docs/xml/`
```

## Notes / Hand Off

- For lifecycle routing before implementation/testing, use `software-vmodel-navigation`.
- For traceability checks, use `software-traceability-audit`.
