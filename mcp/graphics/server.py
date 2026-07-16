"""graphics MCP server — window capture, RenderDoc capture, and render comparison.

Tools:
  graphics_screenshot        capture a window / region to a PNG
  graphics_renderdoc_capture capture a frame with RenderDoc (renderdoccmd)
  graphics_renderdoc_frame   extract a PNG frame from a .rdc capture
  graphics_compare_renders   diff two rendered images + PSNR/SSIM/FLIP metrics

The capture commands are pluggable via environment variables so the server works
across setups: GRAPHICS_SCREENSHOT_CMD, RENDERDOC_CMD. Comparison is local
(numpy/PIL; FLIP-aware if the `flip` package is available).
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "base"))
from locking_store import LockingStore  # noqa: E402

OUT_ROOT = str(Path(__file__).resolve().parent / "data" / "captures")
store = LockingStore(OUT_ROOT)


def _resolve(cmd: str, default: str) -> str:
    return os.environ.get(cmd, default)


def _run(cmd: List[str]) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, capture_output=True, text=True)


# ----------------------------------------------------------------------------
# Tools
# ----------------------------------------------------------------------------
def screenshot(output: str, window: Optional[str] = None, region: Optional[List[int]] = None) -> Dict:
    """Capture a window (or region x,y,w,h) to a PNG via the configured command."""
    out_path = Path(output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    base = _resolve("GRAPHICS_SCREENSHOT_CMD", "")
    if base:
        cmd = base.split() + [str(out_path)]
        if window:
            cmd += ["--window", window]
        if region:
            cmd += ["--region", ",".join(map(str, region))]
        r = _run(cmd)
        if r.returncode != 0:
            raise RuntimeError(f"screenshot failed: {r.stderr}")
    else:
        raise RuntimeError(
            "GRAPHICS_SCREENSHOT_CMD not configured. Set it to a command that takes "
            "the output PNG as its last argument (e.g. a maim/import/scrot wrapper)."
        )
    return {"path": str(out_path), "bytes": out_path.stat().st_size}


def renderdoc_capture(executable: str, output: str, args: Optional[List[str]] = None) -> Dict:
    """Capture one frame of <executable> into a .rdc using renderdoccmd."""
    out_path = Path(output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    rdc = _resolve("RENDERDOC_CMD", "renderdoccmd")
    cmd = [rdc, "capture", "--out", str(out_path), executable] + (args or [])
    r = _run(cmd)
    if r.returncode != 0:
        raise RuntimeError(f"renderdoc capture failed: {r.stderr}")
    return {"path": str(out_path)}


def renderdoc_frame(rdc: str, output: str, frame: int = 0) -> Dict:
    """Extract a PNG from a .rdc capture."""
    out_path = Path(output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    rdc_cmd = _resolve("RENDERDOC_CMD", "renderdoccmd")
    cmd = [rdc_cmd, "replay", "--out", str(out_path), "--frame", str(frame), rdc]
    r = _run(cmd)
    if r.returncode != 0:
        raise RuntimeError(f"renderdoc replay failed: {r.stderr}")
    return {"path": str(out_path)}


def compare_renders(ref: str, candidates: List[str], metrics: Optional[List[str]] = None) -> Dict:
    """Diff a reference image against candidate(s): diff image + PSNR/SSIM/FLIP."""
    from PIL import Image
    import numpy as np

    metrics = metrics or ["psnr", "ssim", "flip"]
    ref_img = Image.open(ref).convert("RGB")
    rw, rh = ref_img.size
    ref_arr = np.asarray(ref_img, dtype=np.float64) / 255.0

    diff_dir = Path(tempfile.mkdtemp(prefix="cmp_", dir=str(Path(OUT_ROOT))))
    side_by_side = diff_dir / "side_by_side.png"

    rows = []
    cand_imgs = []
    for i, c in enumerate(candidates):
        cimg = Image.open(c).convert("RGB")
        if cimg.size != (rw, rh):
            cimg = cimg.resize((rw, rh))
        cand_imgs.append(cimg)
        carr = np.asarray(cimg, dtype=np.float64) / 255.0
        diff = np.abs(ref_arr - carr)
        diff_img = Image.fromarray((diff * 255).clip(0, 255).astype("uint8"))
        diff_path = diff_dir / f"diff_{i}.png"
        diff_img.save(diff_path)

        mse = float(np.mean((ref_arr - carr) ** 2))
        psnr = float(10 * np.log10(1.0 / mse)) if mse > 0 else float("inf")
        # mean SSIM (per-channel, averaged)
        ssim = _ssim(ref_arr, carr)
        row = {"candidate": c, "psnr_db": round(psnr, 3), "ssim": round(float(ssim), 4)}
        # FLIP (perceptual) if available
        if "flip" in metrics:
            row["flip"] = round(float(_flip(ref_arr, carr)), 4)
        rows.append(row)

    # side-by-side: ref | cand0 diff
    if cand_imgs:
        sheet = Image.new("RGB", (rw * 3, rh))
        sheet.paste(ref_img, (0, 0))
        sheet.paste(cand_imgs[0], (rw, 0))
        sheet.paste(Image.open(diff_dir / "diff_0.png"), (rw * 2, 0))
        sheet.save(side_by_side)

    return {
        "reference": ref,
        "diff_images": [str(diff_dir / f"diff_{i}.png") for i in range(len(candidates))],
        "side_by_side": str(side_by_side),
        "metrics": rows,
    }


def _ssim(a: "np.ndarray", b: "np.ndarray", data_range: float = 1.0) -> float:
    import numpy as np

    mu_a, mu_b = a.mean(), b.mean()
    sigma_a2, sigma_b2 = ((a - mu_a) ** 2).mean(), ((b - mu_b) ** 2).mean()
    sigma_ab = ((a - mu_a) * (b - mu_b)).mean()
    c1, c2 = (0.01 * data_range) ** 2, (0.03 * data_range) ** 2
    num = (2 * mu_a * mu_b + c1) * (2 * sigma_ab + c2)
    den = (mu_a**2 + mu_b**2 + c1) * (sigma_a2 + sigma_b2 + c2)
    return float(num / den)


def _flip(a: "np.ndarray", b: "np.ndarray") -> float:
    """FLIP perceptual error (falls back to mean absolute error if pkg absent)."""
    try:
        import flip  # type: ignore  # optional dependency
        import numpy as np
        return float(np.mean(flip.SRGB_to_FLIP(a, b)))  # pragma: no cover
    except Exception:
        import numpy as np

        return float(np.mean(np.abs(a - b)))


# ----------------------------------------------------------------------------
# MCP wiring
# ----------------------------------------------------------------------------
TOOLS = {
    "graphics_screenshot": screenshot,
    "graphics_renderdoc_capture": renderdoc_capture,
    "graphics_renderdoc_frame": renderdoc_frame,
    "graphics_compare_renders": compare_renders,
}
TOOL_SCHEMAS = {
    "graphics_screenshot": {
        "description": "Capture a window or region to a PNG (pluggable command).",
        "inputSchema": {
            "type": "object",
            "properties": {
                "output": {"type": "string"},
                "window": {"type": "string"},
                "region": {"type": "array", "items": {"type": "integer"}},
            },
            "required": ["output"],
        },
    },
    "graphics_renderdoc_capture": {
        "description": "Capture one frame of an executable into a .rdc with RenderDoc.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "executable": {"type": "string"},
                "output": {"type": "string"},
                "args": {"type": "array", "items": {"type": "string"}},
            },
            "required": ["executable", "output"],
        },
    },
    "graphics_renderdoc_frame": {
        "description": "Extract a PNG frame from a .rdc capture.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "rdc": {"type": "string"},
                "output": {"type": "string"},
                "frame": {"type": "integer"},
            },
            "required": ["rdc", "output"],
        },
    },
    "graphics_compare_renders": {
        "description": "Compare a reference render to candidate(s): diff image + PSNR/SSIM/FLIP.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "ref": {"type": "string"},
                "candidates": {"type": "array", "items": {"type": "string"}},
                "metrics": {"type": "array", "items": {"type": "string"}},
            },
            "required": ["ref", "candidates"],
        },
    },
}


def _parse_message(line: str):
    line = line.strip()
    if not line:
        return None
    try:
        return json.loads(line)
    except json.JSONDecodeError:
        return None


def _handle(msg: Dict) -> Optional[Dict]:
    method = msg.get("method")
    mid = msg.get("id")
    if method == "initialize":
        return {"jsonrpc": "2.0", "id": mid, "result": {"protocolVersion": "2024-11-05", "capabilities": {}, "serverInfo": {"name": "graphics", "version": "1.0"}}}
    if method == "notifications/initialized":
        return None
    if method == "tools/list":
        return {"jsonrpc": "2.0", "id": mid, "result": {"tools": [{"name": n, **TOOL_SCHEMAS[n]} for n in TOOLS]}}
    if method == "tools/call":
        name = msg["params"]["name"]
        args = msg["params"].get("arguments", {})
        fn = TOOLS.get(name)
        if fn is None:
            return {"jsonrpc": "2.0", "id": mid, "error": {"code": -32601, "message": f"unknown tool {name}"}}
        try:
            result = fn(**args)
            return {"jsonrpc": "2.0", "id": mid, "result": {"content": [{"type": "text", "text": json.dumps(result, indent=2)}]}}
        except Exception as e:  # noqa: BLE001
            return {"jsonrpc": "2.0", "id": mid, "error": {"code": -32000, "message": str(e)}}
    return None


def main() -> None:
    global store, OUT_ROOT
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=OUT_ROOT)
    args = parser.parse_args()
    OUT_ROOT = args.root
    store = LockingStore(args.root)
    for line in sys.stdin:
        msg = _parse_message(line)
        if msg is None:
            continue
        resp = _handle(msg)
        if resp is not None:
            sys.stdout.write(json.dumps(resp) + "\n")
            sys.stdout.flush()


if __name__ == "__main__":
    main()
