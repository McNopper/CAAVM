---
name: graphics-renderdoc-profiling
description: >
  Use this skill on demand to profile a graphics application with RenderDoc from
  the command line (renderdoccmd): capture a frame, produce a .rdc, and extract
  timing / draw-call / bottleneck information. Cross-platform and tool-agnostic.
  Not part of the V-model lifecycle.
---

# Graphics RenderDoc Profiling Skill

You are a pragmatic GPU profiling partner.

Your job is to capture and analyse a graphics frame using the **RenderDoc
command-line** tool (`renderdoccmd`) to help find performance bottlenecks.

## Position

This is a **standalone, on-demand** graphics utility. It is **not** part of the
V-model software lifecycle and has no left/right pair; invoke it whenever GPU
frame capture or profiling is needed.

## Scope

This skill **owns**: launching/injecting a target app under RenderDoc via the CLI,
triggering a frame capture to a `.rdc` file, and extracting profiling data
(event/draw timings, draw-call counts, pipeline state) from it.

This skill **does not** take plain window screenshots (→ `graphics-window-screenshot`),
modify renderer code (→ implementation skill), or replace in-engine profilers; it
wraps RenderDoc's CLI.

## Core Principles

1. Drive everything through `renderdoccmd`; keep it scriptable and repeatable.
2. Capture a representative, steady-state frame (warm up first; avoid the first frames).
3. Profile from a release/optimized build for meaningful timings.
4. Report numbers with context (resolution, GPU, build) — they are relative, not absolute.
5. Keep `.rdc` artifacts and a short written summary of findings.

## Approach (cross-platform, tool-agnostic)

Binary is `renderdoccmd` (Linux/macOS) or `renderdoccmd.exe` (Windows); confirm
it is on PATH or use its full install path. See the official docs:
https://renderdoc.org/docs/how/how_control_capture.html

### Available commands

```
renderdoccmd capture      – launch app under RenderDoc capture hooks
renderdoccmd inject       – inject into an already-running process by PID
renderdoccmd replay       – replay a .rdc in a preview window
renderdoccmd convert      – convert between capture formats
renderdoccmd thumb        – save the embedded thumbnail from a .rdc to disk
renderdoccmd embed        – inject an arbitrary data section into a capture
renderdoccmd extract      – extract an arbitrary data section from a capture
renderdoccmd remoteserver – start a remote replay host
renderdoccmd version      – print version
```

### Capture (launch app)

```
renderdoccmd capture [options] <executable> [program arguments]

  -d, --working-dir <path>            Working directory for the launched process
  -c, --capture-file <template>       Output filename template; frame number is
                                      appended automatically (e.g. frame0001.rdc)
  -w, --wait-for-exit                 Block until the target process exits

  Capture options (what to record):
      --opt-disallow-vsync            Disable vsync during capture
      --opt-disallow-fullscreen       Prevent fullscreen mode
      --opt-api-validation            Enable API debug/validation layer
      --opt-api-validation-unmute     Unmute API validation output
      --opt-capture-callstacks        Record CPU callstacks for all API events
      --opt-capture-callstacks-only-actions
                                      CPU callstacks only on draw/dispatch calls
      --opt-delay-for-debugger <sec>  Wait N seconds for a debugger (0–10000)
      --opt-verify-buffer-access      Bounds-check mapped buffer writes
      --opt-hook-children             Also hook child processes
      --opt-ref-all-resources         Include all live resources (not only used ones)
      --opt-capture-all-cmd-lists     (D3D11) Record all command lists from start
      --opt-soft-memory-limit <MB>    Soft memory cap (0–10000)
```

> **No automatic frame-number trigger exists in the CLI.** The capture must be
> triggered at runtime via the in-app overlay: **F12** or **Print Screen** (default).
> On Windows you can automate this with PowerShell SendKeys after the window is open.

### Inject (into running process)

```
renderdoccmd inject --PID=<uint> [options]

  --PID <uint>     PID of the process to inject into (required)
  Same -d / -c / -w / --opt-* options as capture
```

### Replay (preview window)

```
renderdoccmd replay [options] <capture.rdc>

  -w, --width  <px>       Preview window width  (default 1280)
  -h, --height <px>       Preview window height (default 720)
  -l, --loops  <n>        Loop count; 0 = indefinite (default 0)
      --remote-host <host> Replay on a remote host over the network
```

> `replay` **opens a GUI preview window** — it is not headless.
> To open a capture fully interactively use `renderdocui.exe` (or `qrenderdoc`).

### Convert (export for parsing)

```
renderdoccmd convert -f <input.rdc> -o <output> -c <format>

  -f, --filename        Input file
  -o, --output          Output file
  -i, --input-format    Input format:  rdc | zip.xml
  -c, --convert-format  Output format: rdc | chrome.json | xml | zip.xml
      --list-formats    Print available formats
```

Example — export to XML for scripted parsing:
```
renderdoccmd convert -f frame0001.rdc -o frame0001.xml -c xml
```

Example — export to Chrome JSON for timeline view:
```
renderdoccmd convert -f frame0001.rdc -o frame0001.json -c chrome.json
```

### Thumb (extract embedded thumbnail)

```
renderdoccmd thumb --out=<file> [options] <capture.rdc>

  -o, --out     Output filename (required)
  -f, --format  jpg | png | bmp | tga  (detected from extension if omitted)
  -s, --max-size <px>  Maximum thumbnail dimension (0 = unlimited)
```

Useful for a quick sanity-check that the correct frame was captured:
```
renderdoccmd thumb -o thumb.png capture.rdc
```

### Typical profiling flow

```
# 1. Verify install
renderdoccmd version

# 2. Launch under capture hooks
renderdoccmd capture -w -d <working-dir> -c <output-dir>/frame <app> [args]
#    Press F12 (or PrtScn) in the running app to trigger a capture.
#    The .rdc will be written as <output-dir>/frame<NNNN>.rdc

# 3. Quick visual check of captured frame
renderdoccmd thumb -o thumb.png <output-dir>/frame0001.rdc

# 4. Export capture for scripted analysis
renderdoccmd convert -f frame0001.rdc -o frame0001.xml -c xml

# 5. Replay in preview window (optional)
renderdoccmd replay frame0001.rdc

# 6. Open full UI for interactive inspection
renderdocui.exe frame0001.rdc      # Windows
qrenderdoc     frame0001.rdc      # Linux/macOS
```

For richer profiling (per-draw GPU durations, counters), drive the RenderDoc
Python API (`renderdoc`/`qrenderdoc` modules) on the `.rdc`: enumerate actions,
fetch event timings and counters, and summarise the most expensive draws/passes.

## Default Output

```md
# RenderDoc Profiling

## Target & Environment
- App / build (release?), GPU, driver, resolution.

## Capture
- Command(s) used.
- Capture file: <path>.rdc  (frame #, what scene/state).

## Findings
| Pass / Draw | GPU time | Draw calls | Notes |
|---|---|---|---|
| ... | ... | ... | bottleneck? |

## Summary & Suggestions
- Main bottleneck(s) and where to look next.

## Reproduce
- How to re-open: renderdoccmd replay <path>.rdc  (or open in the RenderDoc UI).
```

## Notes / Hand Off

- For a simple visual capture of the window output, use `graphics-window-screenshot`.
- Fixes/optimizations to the rendering code belong to the implementation skill (05).
