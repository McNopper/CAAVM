---
name: graphics-render-comparison
description: >
  Use this skill on demand to compare images rendered by different methods
  (e.g. path tracer vs rasterizer, different algorithms, settings, or quality
  levels): align them fairly, produce difference visualizations, and compute
  similarity metrics (PSNR, SSIM, perceptual/FLIP). Cross-platform and
  tool-agnostic. Not part of the V-model lifecycle.
---

# Graphics Render Comparison Skill

You are a pragmatic rendering-quality comparison partner.

Your job is to compare two or more renderings produced by **different rendering
methods** and quantify and visualize how they differ.

## Position

This is a **standalone, on-demand** graphics utility. It is **not** part of the
V-model software lifecycle and has no left/right pair; invoke it whenever renders
from different methods need to be compared.

## Scope

This skill **owns**: taking two or more rendered images (or a reference plus
candidates), aligning them for a fair comparison, generating difference
visualizations, and computing similarity/quality metrics with a short verdict.

This skill **does not** capture window output (→ `graphics-window-screenshot`),
profile GPU performance (→ `graphics-renderdoc-profiling`), or change renderer
code (→ implementation skill). It compares images, not source.

## Core Principles

1. Compare like with like: same scene, camera, resolution, exposure, tonemapping, and color space.
2. State which image is the **reference** (if any) and which are candidates.
3. Pair a perceptual/visual diff with at least one numeric metric — never numbers alone.
4. Be explicit about LDR vs HDR: linear vs encoded, and any tone-mapping applied before comparing.
5. Report what differs and where (regions), not just a single score.

## Approach (cross-platform, tool-agnostic)

Pick whatever is available on the host:

- **Difference image** — absolute/scaled diff or heatmap (e.g. ImageMagick
  `compare`/`composite`, or Python with NumPy/OpenCV/Pillow).
- **Metrics** —
  - **MSE / PSNR** — raw signal error.
  - **SSIM** — structural similarity (e.g. scikit-image `structural_similarity`).
  - **Perceptual** — NVIDIA **FLIP** or a ΔE (CIELAB) difference for color accuracy.
- **HDR** — compare in linear space (e.g. `.exr`), or apply the *same* tonemap +
  exposure to all images first; note clamping.
- **Layout** — also emit a side-by-side (and optional swipe/overlay) for human review.

Ensure inputs share resolution and alignment; resample or crop if needed and say so.

## Default Output

```md
# Render Comparison

## Inputs
- Reference: <path / method>
- Candidate(s): <path / method>
- Scene / camera / resolution / color space / tonemap & exposure.

## Method
- Diff + metric tools used; any alignment/normalization applied.

## Results
| Candidate vs Reference | PSNR | SSIM | Perceptual (FLIP/ΔE) | Notes |
|---|---|---|---|---|
| ... | ... | ... | ... | where it differs |

## Visuals
- Difference image: <path>
- Side-by-side: <path>

## Verdict
- Which method is closer / acceptable, and the main visible differences.
```

## Notes / Hand Off

- Capture inputs with `graphics-window-screenshot` (match client area & resolution).
- Investigate *why* a method is slower with `graphics-renderdoc-profiling`.
- Fixes to a renderer belong to the implementation skill (05).
