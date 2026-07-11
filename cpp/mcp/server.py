#!/usr/bin/env python3
"""cpp MCP server — C++ tooling for opencode.

Dual-purpose:

* **Standalone tools** (`cpp_format`, `cpp_cppcheck`, `cpp_clang_tidy`) run the
  binaries directly on **any** C++ project with caller-supplied settings and
  return structured findings. They do not require the Hephaestus CMake layout.
* **Template tools** (`cpp_configure`, `cpp_build`, `cpp_verify`, `cpp_docs`,
  `cpp_analysis_status`, `cpp_read_report`) drive the canonical CMake targets of
  the Hephaestus `cpp/` AI-first template (or any project that defines them).

The project directory is resolved per call from the `project_dir` argument, else
the `CPP_PROJECT_DIR` environment variable, else the `cpp/` directory that
contains this server (the in-repo default). Standalone tools also take a `path`
(relative to the project dir, or absolute) for the file/directory to analyse.

Binaries are auto-detected on PATH and can be overridden with the `CPPCHECK_BIN`,
`CLANG_TIDY_BIN`, `CLANG_FORMAT_BIN`, and `CMAKE_BIN` environment variables.
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Optional

try:
    import yaml  # PyYAML — optional, needed to parse clang-tidy fixes.yaml
except ImportError:  # pragma: no cover
    yaml = None

from mcp.server.fastmcp import FastMCP

# ---------------------------------------------------------------------------
# Configuration / resolution helpers
# ---------------------------------------------------------------------------

DEFAULT_PROJECT_DIR = Path(__file__).resolve().parent.parent  # the cpp/ template

_CPP_EXT = {
    ".c", ".cc", ".cpp", ".cxx", ".c++", ".h", ".hh", ".hpp", ".hxx", ".h++",
    ".inl", ".ipp", ".tcc",
}
_PRUNE_DIRS = {
    "build", "out", ".git", ".vs", ".idea", "_deps", "external", "third_party",
    "node_modules", "venv", ".venv", ".cache", "cmake-build-debug",
    "cmake-build-release",
}


def _which(explicit: Optional[str], names: list[str]) -> Optional[str]:
    if explicit and shutil.which(explicit):
        return explicit
    for n in names:
        p = shutil.which(n)
        if p:
            return p
    return None


def cppcheck_bin() -> Optional[str]:
    return _which(os.environ.get("CPPCHECK_BIN"), ["cppcheck"])


def clang_tidy_bin() -> Optional[str]:
    return _which(os.environ.get("CLANG_TIDY_BIN"), ["clang-tidy"])


def clang_format_bin() -> Optional[str]:
    return _which(os.environ.get("CLANG_FORMAT_BIN"), ["clang-format"])


def cmake_bin() -> Optional[str]:
    return _which(os.environ.get("CMAKE_BIN"), ["cmake"])


def resolve_project(project_dir: Optional[str]) -> Path:
    if project_dir:
        return Path(project_dir).expanduser().resolve()
    env = os.environ.get("CPP_PROJECT_DIR")
    if env:
        return Path(env).expanduser().resolve()
    return DEFAULT_PROJECT_DIR


def _resolve_path(path: str, project: Path) -> Path:
    p = Path(path)
    return p.resolve() if p.is_absolute() else (project / p).resolve()


def _resolve_files(path: str, project: Path) -> list[Path]:
    """Resolve `path` (a file or a directory) to a list of C++ source files.

    Directory walks prune build/VCS/dependency folders so standalone runs do not
    waste time on generated or third-party code.
    """
    base = _resolve_path(path, project)
    if base.is_file():
        return [base]
    if not base.is_dir():
        return []
    out: list[Path] = []
    for root, dirs, files in os.walk(base):
        dirs[:] = [d for d in dirs if d not in _PRUNE_DIRS]
        for f in files:
            if Path(f).suffix.lower() in _CPP_EXT:
                out.append(Path(root) / f)
    return sorted(out)


def _run(cmd: list[str], cwd: Path, timeout: int = 900) -> tuple[int, str, str]:
    try:
        cp = subprocess.run(
            cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout
        )
        return cp.returncode, cp.stdout, cp.stderr
    except FileNotFoundError:
        return 127, "", f"command not found: {cmd[0]}"
    except subprocess.TimeoutExpired:
        return 124, "", f"timeout after {timeout}s running: {' '.join(cmd[:3])}"


def _tail(text: str, n: int = 60) -> str:
    if not text:
        return ""
    lines = text.splitlines()
    return "\n".join(lines[-n:]) if len(lines) > n else text


# ---------------------------------------------------------------------------
# Parsers
# ---------------------------------------------------------------------------

def _parse_cppcheck_xml(xml_text: str) -> tuple[list[dict], dict]:
    findings: list[dict] = []
    summary = {
        "error": 0, "warning": 0, "style": 0,
        "performance": 0, "portability": 0, "information": 0,
    }
    if not xml_text or not xml_text.strip():
        return findings, summary
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as e:
        return [{"error": f"failed to parse cppcheck XML: {e}"}], summary
    for err in root.findall(".//error"):
        sev = (err.get("severity") or "information").lower()
        if sev in summary:
            summary[sev] += 1
        locs = [
            {
                "file": loc.get("file"),
                "line": _to_int(loc.get("line")),
                "column": _to_int(loc.get("column")),
                "info": loc.get("info"),
            }
            for loc in err.findall("location")
        ]
        findings.append(
            {
                "id": err.get("id"),
                "severity": sev,
                "message": err.get("msg"),
                "cwe": err.get("cwe"),
                "locations": locs,
            }
        )
    return findings, summary


_TIDY_RE = re.compile(
    r"^(?P<file>.+?):(?P<line>\d+):(?P<col>\d+):\s*"
    r"(?P<sev>warning|error|note|fatal error):\s*(?P<msg>.*?)"
    r"(?:\s\[(?P<check>[^\]]+)\])?\s*$"
)


def _parse_tidy_output(text: str) -> list[dict]:
    findings: list[dict] = []
    for line in (text or "").splitlines():
        m = _TIDY_RE.match(line.strip())
        if m:
            findings.append(
                {
                    "file": m.group("file"),
                    "line": int(m.group("line")),
                    "column": int(m.group("col")),
                    "severity": m.group("sev"),
                    "message": m.group("msg"),
                    "check": m.group("check"),
                }
            )
    return findings


def _parse_tidy_fixes(path: Path) -> list[dict]:
    if yaml is None or not path.exists():
        return []
    try:
        data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    except Exception as e:  # pragma: no cover
        return [{"error": f"failed to parse fixes.yaml: {e}"}]
    out = []
    for d in data.get("Diagnostics", []) or []:
        out.append(
            {
                "message": d.get("Message"),
                "file": d.get("FilePath"),
                "offset": d.get("FileOffset"),
                "rule": d.get("DiagnosticName"),
                "replacements": [
                    {
                        "offset": r.get("Offset"),
                        "length": r.get("Length"),
                        "text": r.get("ReplacementText"),
                    }
                    for r in (d.get("Replacements") or [])
                ],
            }
        )
    return out


def _to_int(v) -> Optional[int]:
    try:
        return int(v)
    except (TypeError, ValueError):
        return None


# ---------------------------------------------------------------------------
# Server + tools
# ---------------------------------------------------------------------------

mcp = FastMCP("cpp")


@mcp.tool()
def cpp_tool_list() -> dict:
    """Report which C++ tool binaries are available on PATH (cppcheck,
    clang-tidy, clang-format, cmake). Call this first to see what the other
    tools can do in this environment. Override binaries with the CPPCHECK_BIN,
    CLANG_TIDY_BIN, CLANG_FORMAT_BIN, CMAKE_BIN env vars.
    """
    return {
        "cppcheck": cppcheck_bin(),
        "clang-tidy": clang_tidy_bin(),
        "clang-format": clang_format_bin(),
        "cmake": cmake_bin(),
        "default_project_dir": str(DEFAULT_PROJECT_DIR),
        "cpp_project_dir_env": os.environ.get("CPP_PROJECT_DIR"),
        "pyyaml_available": yaml is not None,
    }


# --- standalone tools (any C++ project, caller-supplied settings) ----------

@mcp.tool()
def cpp_format(
    path: str,
    style: str = "file",
    in_place: bool = True,
    fallback_style: str = "none",
    project_dir: Optional[str] = None,
    timeout: int = 300,
) -> dict:
    """Run **clang-format** on a file or directory.

    Works on any C++ project — no CMake setup required. `path` is a file or
    directory relative to `project_dir` (or absolute); directories are walked
    recursively, pruning build/VCS/dependency folders.

    * `style`: ``"file"`` to use the project's ``.clang-format``, or a named
      style (``llvm``/``google``/``chromium``/``mozilla``/``webkit``/``microsoft``/``gnu``),
      or an inline YAML style string.
    * `in_place`: ``True`` to write changes (format), ``False`` for a dry-run
      check (returns the list of files that need formatting; exit code is
      non-zero if any file is unformatted).
    """
    cf = clang_format_bin()
    if not cf:
        return {"ok": False, "error": "clang-format not found on PATH"}
    project = resolve_project(project_dir)
    files = _resolve_files(path, project)
    if not files:
        return {"ok": False, "error": f"no C++ source files matched '{path}' under {project}"}

    args = [cf, f"--style={style}", f"--fallback-style={fallback_style}"]
    if in_place:
        args.append("-i")
    else:
        args += ["--dry-run", "-Werror"]
    args += [str(f) for f in files]

    rc, out, err = _run(args, project, timeout)
    result = {
        "ok": rc == 0,
        "returncode": rc,
        "in_place": in_place,
        "style": style,
        "files": [str(f) for f in files],
        "binary": cf,
        "tail": _tail(out + err),
    }
    if not in_place:
        # clang-format --dry-run -Werror prints diagnostics as "path:line:col: ...";
        # collect the distinct file paths that need formatting.
        needs = sorted({
            ln.split(":", 1)[0]
            for ln in (err + out).splitlines()
            if ":" in ln and ln.split(":", 1)[0]
        })
        result["needs_formatting"] = needs
    return result


@mcp.tool()
def cpp_cppcheck(
    path: str,
    checks: str = "warning,performance,portability",
    inconclusive: bool = False,
    std: str = "c++17",
    include_dirs: Optional[list[str]] = None,
    defines: Optional[list[str]] = None,
    undefines: Optional[list[str]] = None,
    suppressions: Optional[list[str]] = None,
    suppressions_file: Optional[str] = None,
    platform: str = "native",
    force: bool = False,
    max_configs: int = 50,
    jobs: Optional[int] = None,
    project_dir: Optional[str] = None,
    timeout: int = 900,
) -> dict:
    """Run **cppcheck** on a file or directory with the given settings.

    Works on any C++ project — no CMake setup required. Returns **structured
    findings** parsed from the XML report (id, severity, message, cwe,
    locations) plus a per-severity summary. `path` is a file or directory
    relative to `project_dir` (or absolute).

    * `checks`: cppcheck ``--enable`` value, e.g. ``"warning,performance,portability"``
      (default) or ``"all"``.
    * `std`: language standard (``c++17`` default for portability on existing
      projects; use ``c++20``/``c++23`` as needed).
    * `inconclusive`, `force`, `max_configs`, `jobs`: cppcheck flags.
    * `include_dirs` / `defines` / `undefines` / `suppressions` /
      `suppressions_file`: pass-through project settings.
    """
    cc = cppcheck_bin()
    if not cc:
        return {"ok": False, "error": "cppcheck not found on PATH"}
    project = resolve_project(project_dir)
    target = _resolve_path(path, project)
    if not target.exists():
        return {"ok": False, "error": f"path not found: {target}"}

    args = [
        cc,
        f"--enable={checks}",
        "--xml", "--xml-version=2",
        f"--platform={platform}",
        f"--std={std}",
        f"--max-configs={max_configs}",
        "--inline-suppr",
    ]
    if inconclusive:
        args.append("--inconclusive")
    if force:
        args.append("--force")
    if jobs:
        args += ["-j", str(jobs)]
    for d in include_dirs or []:
        args += ["-I", str(d)]
    for d in defines or []:
        args.append(f"-D{d}")
    for d in undefines or []:
        args.append(f"-U{d}")
    for s in suppressions or []:
        args.append(f"--suppress={s}")
    if suppressions_file:
        args.append(f"--suppressions-list={suppressions_file}")
    args.append(str(target))

    # cppcheck writes the XML to stderr with --xml; human text goes to stdout.
    rc, out, err = _run(args, project, timeout)
    findings, summary = _parse_cppcheck_xml(err)
    return {
        "ok": rc == 0,
        "returncode": rc,
        "checks": checks,
        "std": std,
        "target": str(target),
        "binary": cc,
        "summary": summary,
        "findings_count": len(findings),
        "findings": findings,
        "stdout_tail": _tail(out),
    }


@mcp.tool()
def cpp_clang_tidy(
    path: str,
    compile_commands: Optional[str] = None,
    checks: Optional[str] = None,
    warnings_as_errors: Optional[str] = None,
    header_filter: Optional[str] = None,
    fix: bool = False,
    extra_args: Optional[list[str]] = None,
    project_dir: Optional[str] = None,
    timeout: int = 900,
) -> dict:
    """Run **clang-tidy** on a file or directory.

    Works on any C++ project — no CMake setup required, but full checking needs
    a ``compile_commands.json`` (pass its directory in `compile_commands`, or a
    path to the file). Without it, clang-tidy runs with limited checks; use
    `extra_args` (passed after ``--``) to supply e.g. ``["-std=c++17",
    "-Iinclude"]``. Returns structured findings parsed from the output, plus
    suggested fixes (from ``--export-fixes``) when PyYAML is available.

    * `path`: file or directory relative to `project_dir` (or absolute);
      directories are walked recursively.
    * `checks`: e.g. ``"-*,bugprone-*,performance-*"``.
    * `fix`: apply suggested fixes in place.
    """
    ct = clang_tidy_bin()
    if not ct:
        return {"ok": False, "error": "clang-tidy not found on PATH"}
    project = resolve_project(project_dir)
    files = _resolve_files(path, project)
    if not files:
        return {"ok": False, "error": f"no C++ source files matched '{path}' under {project}"}

    args = [ct]
    if checks:
        args.append(f"--checks={checks}")
    if warnings_as_errors:
        args.append(f"--warnings-as-errors={warnings_as_errors}")
    if header_filter:
        args.append(f"--header-filter={header_filter}")
    if fix:
        args.append("--fix")

    fixes_path: Optional[str] = None
    if yaml is not None:
        tmp = tempfile.NamedTemporaryFile(
            mode="w", suffix=".yaml", prefix="clang-tidy-fixes-", delete=False
        )
        tmp.close()
        fixes_path = tmp.name
        args.append(f"--export-fixes={fixes_path}")

    if compile_commands:
        cc = Path(compile_commands)
        cc_dir = cc.parent if cc.is_file() else cc
        args += ["-p", str(cc_dir)]

    args += [str(f) for f in files]
    if extra_args:
        args += ["--"] + list(extra_args)

    rc, out, err = _run(args, project, timeout)
    findings = _parse_tidy_output(out + err)
    fixes = _parse_tidy_fixes(Path(fixes_path)) if fixes_path else []
    if fixes_path:
        try:
            os.unlink(fixes_path)
        except OSError:
            pass
    return {
        "ok": rc == 0,
        "returncode": rc,
        "checks": checks,
        "files": [str(f) for f in files],
        "binary": ct,
        "findings_count": len(findings),
        "findings": findings,
        "suggested_fixes": fixes,
        "tail": _tail(out + err),
    }


# --- template tools (drive the Hephaestus cpp/ CMake targets) --------------

def _read_analysis_status(build_dir: Path) -> dict:
    f = build_dir / "reports" / "analysis-status.txt"
    if not f.exists():
        return {
            "status": "unknown",
            "reason": f"{f} not found — run cpp_configure first",
            "path": str(f),
        }
    data: dict = {}
    for line in f.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            k, _, v = line.partition("=")
            data[k.strip()] = v.strip()
    data["path"] = str(f)
    return data


def _build_dir_for(project: Path, build_dir: Optional[str], preset: Optional[str]) -> Path:
    if build_dir:
        bd = Path(build_dir)
        return bd.resolve() if bd.is_absolute() else (project / bd).resolve()
    if preset == "release":
        return project / "build-release"
    if preset == "analysis":
        return project / "build-analysis"
    return project / "build"


@mcp.tool()
def cpp_analysis_status(
    build_dir: Optional[str] = None,
    preset: Optional[str] = None,
    project_dir: Optional[str] = None,
) -> dict:
    """Read the AI analysis-status contract of the Hephaestus cpp/ template
    (``${binaryDir}/reports/analysis-status.txt``): ``enabled``|``skipped`` with
    the generator/compiler reason. Tells you whether clang-tidy/cppcheck will
    actually run on this toolchain. Configure the project first.
    """
    project = resolve_project(project_dir)
    bd = _build_dir_for(project, build_dir, preset)
    return _read_analysis_status(bd)


@mcp.tool()
def cpp_configure(
    preset: str = "default",
    project_dir: Optional[str] = None,
    timeout: int = 300,
) -> dict:
    """Configure the Hephaestus cpp/ template with a CMake preset
    (``default`` / ``release`` / ``analysis``). Returns success, the exit code,
    and the analysis-status (enabled/skipped) for the resulting toolchain.
    """
    cm = cmake_bin()
    if not cm:
        return {"ok": False, "error": "cmake not found on PATH"}
    project = resolve_project(project_dir)
    rc, out, err = _run([cm, "--preset", preset], project, timeout)
    bd = _build_dir_for(project, None, preset)
    return {
        "ok": rc == 0,
        "returncode": rc,
        "preset": preset,
        "build_dir": str(bd),
        "analysis": _read_analysis_status(bd),
        "tail": _tail(out + err),
    }


@mcp.tool()
def cpp_build(
    target: Optional[str] = None,
    preset: Optional[str] = None,
    build_dir: Optional[str] = None,
    project_dir: Optional[str] = None,
    timeout: int = 900,
) -> dict:
    """Build the Hephaestus cpp/ template. With `preset`, runs
    ``cmake --build --preset <preset>``; otherwise builds `build_dir` (default
    ``build/``) and optionally a single `target` (e.g. ``verify``, ``tidy``,
    ``cppcheck-xml``, ``docs``, ``format``).
    """
    cm = cmake_bin()
    if not cm:
        return {"ok": False, "error": "cmake not found on PATH"}
    project = resolve_project(project_dir)
    if preset:
        args = [cm, "--build", "--preset", preset]
        if target:
            args += ["--target", target]
        rc, out, err = _run(args, project, timeout)
        return {
            "ok": rc == 0, "returncode": rc, "preset": preset, "target": target,
            "tail": _tail(out + err),
        }
    bd = _build_dir_for(project, build_dir, None)
    args = [cm, "--build", str(bd)]
    if target:
        args += ["--target", target]
    rc, out, err = _run(args, project, timeout)
    return {
        "ok": rc == 0, "returncode": rc, "build_dir": str(bd), "target": target,
        "tail": _tail(out + err),
    }


@mcp.tool()
def cpp_verify(
    full: bool = False,
    project_dir: Optional[str] = None,
    timeout: int = 900,
) -> dict:
    """Run the Hephaestus cpp/ template verification target:
    ``verify`` (fast: build + tests + analysis status) or ``verify-full``
    (strict: verify + format-check + static analysis + docs). Returns the
    pass/fail, exit code, and analysis-status.
    """
    cm = cmake_bin()
    if not cm:
        return {"ok": False, "error": "cmake not found on PATH"}
    project = resolve_project(project_dir)
    bd = _build_dir_for(project, None, None)
    target = "verify-full" if full else "verify"
    rc, out, err = _run([cm, "--build", str(bd), "--target", target], project, timeout)
    return {
        "ok": rc == 0,
        "returncode": rc,
        "target": target,
        "analysis": _read_analysis_status(bd),
        "tail": _tail(out + err),
    }


@mcp.tool()
def cpp_docs(
    project_dir: Optional[str] = None,
    timeout: int = 300,
) -> dict:
    """Generate Doxygen docs for the Hephaestus cpp/ template (``docs`` target).
    Returns the XML/tagfile paths and the Doxygen warnings list.
    """
    cm = cmake_bin()
    if not cm:
        return {"ok": False, "error": "cmake not found on PATH"}
    project = resolve_project(project_dir)
    bd = _build_dir_for(project, None, "analysis")
    rc, out, err = _run([cm, "--build", str(bd), "--target", "docs"], project, timeout)
    warnlog = bd / "docs" / "doxygen_warnings.log"
    warnings: list[str] = []
    if warnlog.exists():
        warnings = [
            ln for ln in warnlog.read_text(encoding="utf-8", errors="replace").splitlines()
            if ln.strip()
        ]
    return {
        "ok": rc == 0,
        "returncode": rc,
        "xml_path": str(bd / "docs" / "xml"),
        "tagfile": str(bd / "docs" / "MyProject.tag"),
        "warnings_count": len(warnings),
        "warnings": warnings[:200],
        "tail": _tail(out + err),
    }


@mcp.tool()
def cpp_read_report(
    name: str,
    build_dir: Optional[str] = None,
    preset: Optional[str] = None,
    project_dir: Optional[str] = None,
) -> dict:
    """Read a named machine-readable report produced by the Hephaestus cpp/
    template, returning parsed content. `name` is one of:
    ``analysis-status``, ``clang-tidy-fixes``, ``cppcheck-xml``,
    ``doxygen-warnings``.
    """
    project = resolve_project(project_dir)
    bd = _build_dir_for(project, build_dir, preset)
    mapping = {
        "analysis-status": (bd / "reports" / "analysis-status.txt", "kv"),
        "clang-tidy-fixes": (bd / "reports" / "clang-tidy" / "fixes.yaml", "yaml"),
        "cppcheck-xml": (bd / "reports" / "cppcheck" / "cppcheck.xml", "cppcheck"),
        "doxygen-warnings": (bd / "docs" / "doxygen_warnings.log", "lines"),
    }
    if name not in mapping:
        return {"error": f"unknown report '{name}'. valid: {sorted(mapping)}"}
    f, kind = mapping[name]
    if not f.exists():
        return {"error": f"{f} not found — build the relevant target first", "path": str(f)}
    text = f.read_text(encoding="utf-8", errors="replace")
    if kind == "kv":
        out = {}
        for line in text.splitlines():
            if "=" in line:
                k, _, v = line.partition("=")
                out[k.strip()] = v.strip()
        return out
    if kind == "yaml":
        return {"findings": _parse_tidy_fixes(f)} if yaml else {"raw": text, "error": "pyyaml not installed"}
    if kind == "cppcheck":
        findings, summary = _parse_cppcheck_xml(text)
        return {"summary": summary, "findings_count": len(findings), "findings": findings}
    return {"lines": [ln for ln in text.splitlines() if ln.strip()]}


if __name__ == "__main__":
    mcp.run()
