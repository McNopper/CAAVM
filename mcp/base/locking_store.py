"""Reusable, concurrency-safe JSON document store for MCP servers.

Supports multiple independent *projects*: every document is scoped by a
``project`` key, and all access is guarded by a re-entrant threading lock plus a
cross-process file lock, with atomic temp-then-rename writes.

This module is used by the pm MCP server.
"""
from __future__ import annotations

import json
import os
import tempfile
import threading
from contextlib import contextmanager
from pathlib import Path
from typing import Any, Callable, Dict, Iterator, List, Optional

try:
    import filelock
except ImportError:  # pragma: no cover - dependency guidance
    raise RuntimeError(
        "The 'filelock' package is required. Install it (e.g. pip install filelock)."
    )


class DocumentNotFound(Exception):
    pass


class LockingStore:
    """A directory of JSON documents, one file per project, with locking IO.

    Layout::

        <root>/
          <project>.json        # the document for a single project
          <project>.json.lock   # filelock lock file (auto-managed)

    The whole-document lock means two processes/threads never read-then-write
    a stale version: every mutation goes through ``transaction``.
    """

    def __init__(self, root: str | os.PathLike, default_project: str = "default") -> None:
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)
        self.default_project = default_project
        self._thread_lock = threading.RLock()

    # -- paths -----------------------------------------------------------
    def _doc_path(self, project: str) -> Path:
        safe = project.replace("/", "_").replace("\\", "_")
        return self.root / f"{safe}.json"

    def _lock_path(self, project: str) -> Path:
        return self._doc_path(project).with_suffix(".json.lock")

    # -- raw IO (callers must hold the lock) -----------------------------
    def _read_raw(self, project: str) -> Dict[str, Any]:
        path = self._doc_path(project)
        if not path.exists():
            return {}
        with path.open("r", encoding="utf-8") as fh:
            return json.load(fh)

    def _write_raw(self, project: str, data: Dict[str, Any]) -> None:
        path = self._doc_path(project)
        fd, tmp = tempfile.mkstemp(dir=str(self.root), suffix=".tmp")
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as fh:
                json.dump(data, fh, indent=2, ensure_ascii=False)
                fh.flush()
                os.fsync(fh.fileno())
            os.replace(tmp, path)  # atomic on same filesystem
        finally:
            if os.path.exists(tmp):
                os.remove(tmp)

    # -- transactions ----------------------------------------------------
    @contextmanager
    def transaction(
        self, project: Optional[str] = None
    ) -> Iterator[Dict[str, Any]]:
        """Yield the project document; persist it on clean exit.

        The block may mutate the yielded dict in place. On normal exit the
        mutated document is written atomically. On exception nothing is written.
        """
        project = project or self.default_project
        lock = filelock.FileLock(str(self._lock_path(project)), timeout=30)
        with self._thread_lock:
            with lock:
                data = self._read_raw(project)
                yield data
                self._write_raw(project, data)

    def read(self, project: Optional[str] = None) -> Dict[str, Any]:
        """Read-only snapshot (still locked)."""
        project = project or self.default_project
        lock = filelock.FileLock(str(self._lock_path(project)), timeout=30)
        with self._thread_lock:
            with lock:
                return self._read_raw(project)
