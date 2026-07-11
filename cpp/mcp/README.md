# cpp MCP server

A small [MCP](https://modelcontextprotocol.io/) server that exposes C++ tooling to
opencode agents as **structured tools returning JSON** — so an agent calls a tool and
gets parsed findings back, instead of shelling out to `cmake`/`cppcheck`/`clang-*` and
parsing log text itself.

It is **dual-purpose**:

- **New project — full Hephaestus `cpp/` template.** Drive the canonical CMake targets
  (`verify`, `verify-full`, `tidy`, `cppcheck*`, `docs`, …) and read the machine-readable
  reports under `build/reports/`.
- **Existing C++ project — standalone analysis.** Run `cppcheck`, `clang-format`, and
  `clang-tidy` **directly** with your own settings on any file or directory. No CMake or
  Hephaestus layout required.

## Requirements

The server itself needs **Python 3.10+** on PATH (the MCP Python SDK requires 3.10+).
The C++ tools are **optional but expected on PATH** — install the ones you want to use:

| Tool | Used by | Where to get it |
|---|---|---|
| [**cppcheck**](https://github.com/danmar/cppcheck) | `cpp_cppcheck`, template `cppcheck*` targets | <https://github.com/danmar/cppcheck> — `choco install cppcheck` / `brew install cppcheck` / `sudo apt install cppcheck`, or a release tarball from the GitHub releases page. |
| **clang-format**, **clang-tidy** | `cpp_format`, `cpp_clang_tidy`, template `tidy`/`format` targets | Ship with an **LLVM/Clang** install — they must be on PATH. Windows: the "C++ Clang tools for Windows" VS component, or the LLVM installer from <https://releases.llvm.org>. macOS: `brew install llvm` (and put `$(brew --prefix llvm)/bin` on PATH). Linux: your distro's `clang` / `clang-tools` package or the LLVM apt repo. |
| [**CMake**](https://cmake.org/download/) ≥ 3.26 | template tools (`cpp_configure`, `cpp_build`, `cpp_verify`, `cpp_docs`) | <https://cmake.org/download/> — ensure `cmake` is on PATH. |
| [**Ninja**](https://ninja-build.org/) | recommended generator for the template (unlocks the full analysis stack) | <https://ninja-build.org/> — `choco install ninja` / `brew install ninja` / `sudo apt install ninja-build`. |

Notes:

- On the template, clang-tidy/cppcheck analysis is `enabled` only with Ninja/Makefiles
  + a Clang/GNU-compatible compiler; otherwise it is cleanly `skipped` (not an error).
- Override any binary with the `CPPCHECK_BIN` / `CLANG_TIDY_BIN` / `CLANG_FORMAT_BIN` /
  `CMAKE_BIN` env vars (e.g. to point at a specific LLVM version).
- Call **`cpp_tool_list`** to see exactly which binaries the server found on PATH.

## Install

```bash
pip install -r cpp/mcp/requirements.txt   # installs: mcp, PyYAML
```

The server ships **enabled by default** in this repo's `opencode.json`:

```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "cpp": {
      "type": "local",
      "command": ["python", "cpp/mcp/server.py"],
      "enabled": true
    }
  }
}
```

If the Python deps are not installed, the server prints a one-line "install
requirements" hint and exits — opencode keeps working, just without the `cpp`
tools. To disable it entirely, set `enabled: false` (or drop the `mcp.cpp` block).

> After any change to `opencode.json` or the server, **restart opencode** — config and
> MCP servers load once at startup.

### Pointing it at a project

The project directory is resolved, in priority order, from:

1. the **per-call `project_dir`** argument (every tool accepts it),
2. the **`CPP_PROJECT_DIR`** environment variable,
3. the in-repo default — the `cpp/` folder that contains this server.

For a standalone run on a different project, set `CPP_PROJECT_DIR` (or pass `project_dir`
per call). The standalone analysis tools also take a `path` (file or directory, relative
to the project dir or absolute).

## Use in an existing project (cppcheck / clang-format / clang-tidy)

Copy `cpp/mcp/` into your project (or keep one global install), wire `mcp.cpp` as above
with `env: { "CPP_PROJECT_DIR": "/path/to/your/project" }`, then ask opencode, e.g.:

- *"Run cppcheck on `src/` with `warning,performance,portability`, std c++17, ignore `third_party/`."*
  → `cpp_cppcheck(path="src", checks="warning,performance,portability", std="c++17",
     suppressions=["something"], include_dirs=["include"])` → structured findings + a
  per-severity summary.
- *"Check formatting using the project's `.clang-format` and list what needs fixing."*
  → `cpp_format(path=".", style="file", in_place=False)` → `needs_formatting: [...]`.
- *"Apply clang-format in place across `src` and `include`."*
  → `cpp_format(path="src", in_place=True)` (+ a second call for `include`).
- *"Run clang-tidy with my `compile_commands.json`, checks `bugprone-*,performance-*`."*
  → `cpp_clang_tidy(path="src", compile_commands="build", checks="bugprone-*,performance-*")`
  → findings + suggested fixes.

Directory walks prune build/VCS/dependency folders (`build`, `build-*`, `out`, `.git`,
`_deps`, `external`, `third_party`, …).

## Use with the Hephaestus `cpp/` template (full lifecycle)

| Tool | Does | Returns |
|---|---|---|
| `cpp_analysis_status` | read `reports/analysis-status.txt` | `enabled`\|`skipped` + generator/compiler reason |
| `cpp_configure` | `cmake --preset default\|release\|analysis` | ok + analysis status |
| `cpp_build` | `cmake --build` (preset / build dir / target) | ok + tail |
| `cpp_verify` | `verify` / `verify-full` target | pass/fail + analysis status |
| `cpp_docs` | `docs` target | Doxygen XML/tagfile paths + warnings |
| `cpp_read_report` | parse a named report | structured content |

…in addition to the standalone `cpp_cppcheck` / `cpp_format` / `cpp_clang_tidy`, which
work here too.

Typical flow:

1. `cpp_configure(preset="default")`
2. `cpp_verify()` — fast build + tests + analysis status, or `cpp_verify(full=True)` for
   the strict gate.
3. `cpp_read_report(name="clang-tidy-fixes")` / `"cppcheck-xml"` to inspect findings.

`verify` always reports whether analysis is `enabled` or `skipped` for the toolchain;
`enabled` needs Ninja/Makefiles + a Clang/GNU-compatible compiler.

## Tool reference (standalone)

| Tool | Key parameters |
|---|---|
| `cpp_cppcheck` | `path`, `checks`, `std`, `inconclusive`, `force`, `include_dirs`, `defines`, `undefines`, `suppressions`, `suppressions_file`, `platform`, `jobs` |
| `cpp_format` | `path`, `style` (`file`/named/inline YAML), `in_place`, `fallback_style` |
| `cpp_clang_tidy` | `path`, `compile_commands`, `checks`, `warnings_as_errors`, `header_filter`, `fix`, `extra_args` |
| `cpp_tool_list` | (none) — reports which binaries are available |

All tools accept `project_dir` and `timeout`.

## Development notes

- The server speaks MCP over stdio (`FastMCP`); opencode spawns it via the `command`.
- It has no build step — run `python cpp/mcp/server.py` directly. A quick smoke test:
  `python -c "import ast,sys; ast.parse(open('cpp/mcp/server.py',encoding='utf-8').read())"`.
- `PyYAML` is optional; without it, clang-tidy suggested-fixes and `clang-tidy-fixes`
  reports return raw text instead of parsed structures. cppcheck XML uses the standard
  library, so cppcheck parsing always works.
