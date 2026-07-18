"""pm MCP server — concrete Scrum-like ticket & sprint management.

Every ticket and sprint is scoped to a *project* so multiple independent
projects can coexist. Access is concurrency-safe (see mcp.base.locking_store).

Protocol note: a deliberately minimal stdio JSON-RPC loop (``initialize``,
``tools/list``, ``tools/call``) — no ``resources``/``prompts``, cancellation,
or progress. Enough for opencode tool calls; not a fully featured MCP server.

Document shape (per project)::

    {
      "tickets": { "<id>": { ...ticket... } },
      "sprints": { "<id>": { ...sprint... } },
      "counter": <int>,               # for id generation
      "seq":     { "<prefix>": <int> }  # per-prefix counters (T, FR, ...)
    }
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "base"))
from locking_store import LockingStore  # noqa: E402

STORE_ROOT = str(Path(__file__).resolve().parent / "data")
store = LockingStore(STORE_ROOT)

# The documented default set of disciplines. ``role`` is an OPEN, extensible
# string — the server accepts any non-empty value so a new discipline can be
# used without editing this list (just keep the routing in
# pm-orchestrate-execution in sync). KNOWN_ROLES is only a hint for clients.
KNOWN_ROLES = [
    "architect", "developer", "tester", "pm",
    "cpp-engineer", "graphics-engineer",
]
# Used by audit_traceability to pair definition-side and verification-side work.
DEFINITION_ROLES = {"architect", "developer"}
VERIFICATION_ROLES = {"tester"}
VALID_TYPES = ["story", "task", "bug", "spike"]
VALID_STATUSES = [
    "product-backlog", "sprint-backlog", "in-progress", "in-review", "done",
]
PRIORITY_ORDER = {"low": 0, "medium": 1, "high": 2, "critical": 3}


def _now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def _next_id(project: str, prefix: str = "T") -> str:
    with store.transaction(project) as doc:
        doc.setdefault("seq", {})
        n = doc["seq"].get(prefix, 0) + 1
        doc["seq"][prefix] = n
        return f"{prefix}-{n:03d}"


def _next_sprint(project: str) -> str:
    with store.transaction(project) as doc:
        doc.setdefault("counter", 0)
        doc["counter"] += 1
        return f"S-{doc['counter']:02d}"


def _append_history(ticket: Dict[str, Any], action: str, by: Optional[str]) -> None:
    ticket.setdefault("history", []).append(
        {"ts": _now(), "action": action, "by": by}
    )


def _new_ticket(
    project: str,
    title: str,
    description: str = "",
    type: str = "task",
    role: str = "developer",
    priority: str = "medium",
    story_points: int = 0,
    acceptance_criteria: Optional[List[str]] = None,
    labels: Optional[List[str]] = None,
    epic: Optional[str] = None,
    id_prefix: str = "T",
) -> Dict[str, Any]:
    # role is an open, extensible discipline string: accept any non-empty
    # value. KNOWN_ROLES is only a client-side hint, not a hard gate.
    if not isinstance(role, str) or not role.strip():
        raise ValueError(f"role must be a non-empty string, got {role!r}")
    if type not in VALID_TYPES:
        raise ValueError(f"type must be one of {VALID_TYPES}, got {type!r}")
    if priority not in PRIORITY_ORDER:
        raise ValueError(f"priority must be one of {list(PRIORITY_ORDER)}, got {priority!r}")
    tid = _next_id(project, id_prefix)
    ticket = {
        "id": tid,
        "title": title,
        "description": description,
        "type": type,
        "status": "product-backlog",
        "blocked": False,
        "blocker": None,
        "sprint": None,
        "story_points": story_points,
        "priority": priority,
        "role": role,
        "assignee": None,
        "acceptance_criteria": acceptance_criteria or [],
        "labels": labels or [],
        "epic": epic,
        "artifacts": [],
        "todos": [],
        "created_at": _now(),
        "updated_at": _now(),
        "history": [],
        "comments": [],
    }
    _append_history(ticket, "created", None)
    return ticket


# ----------------------------------------------------------------------------
# Tool implementations
# ----------------------------------------------------------------------------
def create_ticket(project: str, **kw) -> Dict[str, Any]:
    ticket = _new_ticket(project, **kw)
    with store.transaction(project) as doc:
        doc.setdefault("tickets", {})[ticket["id"]] = ticket
    return ticket


def get_ticket(project: str, ticket_id: str) -> Dict[str, Any]:
    doc = store.read(project)
    t = doc.get("tickets", {}).get(ticket_id)
    if t is None:
        raise KeyError(f"ticket {ticket_id} not found in project {project}")
    return t


def list_tickets(
    project: str,
    role: Optional[str] = None,
    status: Optional[str] = None,
    sprint: Optional[str] = None,
    blocked: Optional[bool] = None,
) -> List[Dict[str, Any]]:
    doc = store.read(project)
    out = list(doc.get("tickets", {}).values())
    if role is not None:
        out = [t for t in out if t.get("role") == role]
    if status is not None:
        out = [t for t in out if t.get("status") == status]
    if sprint is not None:
        out = [t for t in out if t.get("sprint") == sprint]
    if blocked is not None:
        out = [t for t in out if t.get("blocked") == blocked]
    return out


def update_ticket(project: str, ticket_id: str, **changes) -> Dict[str, Any]:
    protected = {"id", "created_at", "history", "comments"}
    changes = {k: v for k, v in changes.items() if k not in protected}
    if "role" in changes and (not isinstance(changes["role"], str) or not changes["role"].strip()):
        raise ValueError("role must be a non-empty string")
    if "status" in changes and changes["status"] not in VALID_STATUSES:
        raise ValueError(f"status must be one of {VALID_STATUSES}")
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found")
        for k, v in changes.items():
            t[k] = v
        t["updated_at"] = _now()
        _append_history(t, f"updated:{','.join(changes)}", None)
        return t


def set_blocked(project: str, ticket_id: str, blocker: str, by: Optional[str] = None) -> Dict[str, Any]:
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found")
        t["blocked"] = True
        t["blocker"] = blocker
        t["updated_at"] = _now()
        _append_history(t, f"blocked:{blocker}", by)
        return t


def clear_blocked(project: str, ticket_id: str, by: Optional[str] = None) -> Dict[str, Any]:
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found")
        t["blocked"] = False
        t["blocker"] = None
        t["updated_at"] = _now()
        _append_history(t, "unblocked", by)
        return t


def claim_ticket(
    project: str, role: str, status: str = "sprint-backlog", by: Optional[str] = None
) -> Optional[Dict[str, Any]]:
    """Atomically find the next matching ticket, move it to in-progress, set assignee.

    Returns the claimed ticket, or None if nothing is claimable. Two concurrent
    callers always receive different tickets.
    """
    claimable = ("sprint-backlog",)
    if status not in claimable:
        raise ValueError(
            f"claim status must be one of {claimable}, got {status!r} "
            "(only sprint-backlog tickets are claimable)"
        )
    with store.transaction(project) as doc:
        tickets = list(doc.get("tickets", {}).values())
        # Priority: unblocked, by priority desc, then created_at asc.
        candidates = [
            t for t in tickets
            if t.get("role") == role and t.get("status") == status and not t.get("blocked")
        ]
        if not candidates:
            return None
        candidates.sort(
            key=lambda t: (-PRIORITY_ORDER.get(t.get("priority", "low"), 0), t.get("created_at", ""))
        )
        t = candidates[0]
        t["status"] = "in-progress"
        t["assignee"] = by or role
        t["updated_at"] = _now()
        _append_history(t, f"claimed by {by or role}", by)
        doc["tickets"][t["id"]] = t
        return t


def release_ticket(project: str, ticket_id: str, by: Optional[str] = None) -> Dict[str, Any]:
    """Return an unstarted (or in-progress) ticket to sprint-backlog; clear assignee."""
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found")
        if t.get("status") not in ("sprint-backlog", "in-progress"):
            raise ValueError(f"cannot release ticket in status {t.get('status')!r}")
        t["status"] = "sprint-backlog"
        t["assignee"] = None
        t["updated_at"] = _now()
        _append_history(t, f"released by {by or '?' }", by)
        return t


def add_comment(project: str, ticket_id: str, comment: str, by: Optional[str] = None) -> Dict[str, Any]:
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found")
        t.setdefault("comments", []).append({"ts": _now(), "by": by, "text": comment})
        t["updated_at"] = _now()
        return t


def add_artifact(
    project: str,
    ticket_id: str,
    kind: str,
    ref: str,
    note: str = "",
    by: Optional[str] = None,
) -> Dict[str, Any]:
    """Record where a produced artifact lives so the next agent can find it.

    ``kind`` is one of: ``file`` (a path on disk), ``git`` (commit/branch/ref),
    ``path`` (a directory or generic path), ``url`` (remote resource),
    ``doc`` (a document/artifact id). The ``ref`` is the locator; ``note`` is a
    human hint ("the implemented module", "review branch", "diff image").
    """
    valid = {"file", "git", "path", "url", "doc"}
    if kind not in valid:
        raise ValueError(f"kind must be one of {sorted(valid)}, got {kind!r}")
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found")
        art = {"kind": kind, "ref": ref, "note": note, "by": by, "ts": _now()}
        t.setdefault("artifacts", []).append(art)
        t["updated_at"] = _now()
        _append_history(t, f"artifact:{kind}:{ref}", by)
        return t


def add_todo(project: str, ticket_id: str, text: str, done: bool = False, by: Optional[str] = None) -> Dict[str, Any]:
    """Append a todo (checklist item {text, done}) to a ticket."""
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found in project {project}")
        t.setdefault("todos", []).append({"text": text, "done": done})
        t["updated_at"] = _now()
        _append_history(t, f"todo_added:{text}", by)
        return t


def toggle_todo(project: str, ticket_id: str, index: int, by: Optional[str] = None) -> Dict[str, Any]:
    """Flip the done flag of a ticket's todo by 0-based index."""
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found in project {project}")
        todos = t.setdefault("todos", [])
        if not 0 <= index < len(todos):
            raise IndexError(f"todo index {index} out of range (have {len(todos)})")
        todos[index]["done"] = not todos[index].get("done", False)
        t["updated_at"] = _now()
        _append_history(t, f"todo_toggled:{index}", by)
        return t


def remove_todo(project: str, ticket_id: str, index: int, by: Optional[str] = None) -> Dict[str, Any]:
    """Remove a ticket's todo by 0-based index."""
    with store.transaction(project) as doc:
        t = doc.get("tickets", {}).get(ticket_id)
        if t is None:
            raise KeyError(f"ticket {ticket_id} not found in project {project}")
        todos = t.setdefault("todos", [])
        if not 0 <= index < len(todos):
            raise IndexError(f"todo index {index} out of range (have {len(todos)})")
        removed = todos.pop(index)
        t["updated_at"] = _now()
        _append_history(t, f"todo_removed:{removed.get('text')}", by)
        return t


def get_backlog(project: str) -> List[Dict[str, Any]]:
    """Prioritized product backlog (product-backlog status only)."""
    out = list_tickets(project, status="product-backlog")
    out.sort(key=lambda t: (-PRIORITY_ORDER.get(t.get("priority", "low"), 0), t.get("created_at", "")))
    return out


def get_board(project: str, sprint: Optional[str] = None) -> Dict[str, List[Dict[str, Any]]]:
    """Sprint Kanban grouped by status."""
    out: Dict[str, List[Dict[str, Any]]] = {s: [] for s in VALID_STATUSES}
    for t in list_tickets(project, sprint=sprint):
        if t.get("status") in out:
            out[t["status"]].append(t)
    return out


def plan_sprint(project: str, sprint_id: Optional[str] = None, ticket_ids: Optional[List[str]] = None,
                goal: str = "") -> Dict[str, Any]:
    """Create a sprint (or use given id) and commit given tickets into it."""
    sid = sprint_id or _next_sprint(project)
    with store.transaction(project) as doc:
        doc.setdefault("sprints", {})
        sprint = doc["sprints"].get(sid) or {
            "id": sid, "goal": goal, "status": "active",
            "created_at": _now(), "closed_at": None,
        }
        sprint["goal"] = goal or sprint.get("goal", "")
        if ticket_ids:
            for tid in ticket_ids:
                t = doc["tickets"].get(tid)
                if t is None:
                    raise KeyError(f"ticket {tid} not found")
                t["sprint"] = sid
                t["status"] = "sprint-backlog"
                t["updated_at"] = _now()
                _append_history(t, f"planned into {sid}", None)
        doc["sprints"][sid] = sprint
        return sprint


def close_sprint(project: str, sprint_id: str) -> Dict[str, Any]:
    """Close a sprint; unfinished tickets return to product-backlog."""
    with store.transaction(project) as doc:
        sprint = doc.get("sprints", {}).get(sprint_id)
        if sprint is None:
            raise KeyError(f"sprint {sprint_id} not found")
        returned: List[str] = []
        for tid, t in doc.get("tickets", {}).items():
            if t.get("sprint") == sprint_id and t.get("status") != "done":
                t["sprint"] = None
                t["status"] = "product-backlog"
                t["updated_at"] = _now()
                _append_history(t, f"returned from {sprint_id}", None)
                returned.append(tid)
        sprint["status"] = "closed"
        sprint["closed_at"] = _now()
        doc["sprints"][sprint_id] = sprint
        return {"sprint": sprint, "returned_to_backlog": returned}


def audit_traceability(project: str) -> Dict[str, Any]:
    """Map definition-side role tickets to verification-side role tickets.

    Beyond the raw ``epic`` -> children links, this surfaces per ticket:
      * ``verifies``   - for a verification-role ticket, the definition id it
        covers (its ``epic``), when that definition exists;
      * ``verified_by`` - for a definition-role ticket, the verification ticket
        ids that name it as their ``epic``;
    and at the project level:
      * ``orphan_definitions``     - definition tickets with no verification child;
      * ``orphan_verifications``   - verification tickets whose ``epic`` is
        missing or points at a non-existent ticket.
    """
    tickets = list_tickets(project)
    by_id = {t.get("id"): t for t in tickets}
    matrix = []
    orphan_definitions: List[str] = []
    orphan_verifications: List[str] = []
    for t in tickets:
        tid = t.get("id")
        role = t.get("role")
        children = [o for o in tickets if o.get("epic") == tid]
        links = [o["id"] for o in children]
        verifies: Optional[str] = None
        verified_by: List[str] = []
        if role in VERIFICATION_ROLES:
            epic = t.get("epic")
            verifies = epic if (epic and epic in by_id) else None
            if not verifies:
                orphan_verifications.append(tid)
        if role in DEFINITION_ROLES:
            verified_by = [o["id"] for o in children if o.get("role") in VERIFICATION_ROLES]
            if not verified_by:
                orphan_definitions.append(tid)
        matrix.append({
            "id": tid,
            "role": role,
            "status": t.get("status"),
            "epic": t.get("epic"),
            "links": links,
            "verifies": verifies,
            "verified_by": verified_by,
        })
    return {
        "matrix": matrix,
        "ticket_count": len(tickets),
        "orphan_definitions": orphan_definitions,
        "orphan_verifications": orphan_verifications,
    }


# ----------------------------------------------------------------------------
# MCP wiring (stdio JSON-RPC, minimal)
# ----------------------------------------------------------------------------
TOOLS = {
    "pm_create_ticket": create_ticket,
    "pm_get_ticket": get_ticket,
    "pm_list_tickets": list_tickets,
    "pm_update_ticket": update_ticket,
    "pm_set_blocked": set_blocked,
    "pm_clear_blocked": clear_blocked,
    "pm_claim_ticket": claim_ticket,
    "pm_release_ticket": release_ticket,
    "pm_add_comment": add_comment,
    "pm_add_artifact": add_artifact,
    "pm_add_todo": add_todo,
    "pm_toggle_todo": toggle_todo,
    "pm_remove_todo": remove_todo,
    "pm_get_backlog": get_backlog,
    "pm_get_board": get_board,
    "pm_plan_sprint": plan_sprint,
    "pm_close_sprint": close_sprint,
    "pm_audit_traceability": audit_traceability,
}

TOOL_SCHEMAS = {
    "pm_create_ticket": {
        "description": "Create a ticket in a project (status=product-backlog).",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "title": {"type": "string"},
                "description": {"type": "string"},
                "type": {"enum": VALID_TYPES},
                "role": {"type": "string", "description": "Discipline that owns/claims the ticket. Known roles: architect, developer, tester, pm, cpp-engineer, graphics-engineer (extensible - any non-empty string accepted)."},
                "priority": {"enum": list(PRIORITY_ORDER)},
                "story_points": {"type": "integer"},
                "acceptance_criteria": {"type": "array", "items": {"type": "string"}},
                "labels": {"type": "array", "items": {"type": "string"}},
                "epic": {"type": "string"},
                "id_prefix": {"type": "string"},
            },
            "required": ["project", "title"],
        },
    },
    "pm_get_ticket": {
        "description": "Get one ticket by id.",
        "inputSchema": {
            "type": "object",
            "properties": {"project": {"type": "string"}, "ticket_id": {"type": "string"}},
            "required": ["project", "ticket_id"],
        },
    },
    "pm_list_tickets": {
        "description": "List tickets, optionally filtered by role/status/sprint/blocked.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "role": {"type": "string", "description": "Discipline that owns/claims the ticket. Known roles: architect, developer, tester, pm, cpp-engineer, graphics-engineer (extensible - any non-empty string accepted)."},
                "status": {"enum": VALID_STATUSES},
                "sprint": {"type": "string"},
                "blocked": {"type": "boolean"},
            },
            "required": ["project"],
        },
    },
    "pm_update_ticket": {
        "description": "Update mutable fields of a ticket.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "ticket_id": {"type": "string"},
                "title": {"type": "string"},
                "description": {"type": "string"},
                "type": {"enum": VALID_TYPES},
                "status": {"enum": VALID_STATUSES},
                "story_points": {"type": "integer"},
                "priority": {"enum": list(PRIORITY_ORDER)},
                "role": {"type": "string", "description": "Discipline that owns/claims the ticket. Known roles: architect, developer, tester, pm, cpp-engineer, graphics-engineer (extensible - any non-empty string accepted)."},
                "assignee": {"type": "string"},
                "acceptance_criteria": {"type": "array", "items": {"type": "string"}},
                "labels": {"type": "array", "items": {"type": "string"}},
                "epic": {"type": "string"},
            },
            "required": ["project", "ticket_id"],
        },
    },
    "pm_set_blocked": {
        "description": "Mark a ticket blocked with a reason (orthogonal flag).",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "ticket_id": {"type": "string"},
                "blocker": {"type": "string"},
                "by": {"type": "string"},
            },
            "required": ["project", "ticket_id", "blocker"],
        },
    },
    "pm_clear_blocked": {
        "description": "Clear the blocked flag on a ticket.",
        "inputSchema": {
            "type": "object",
            "properties": {"project": {"type": "string"}, "ticket_id": {"type": "string"}, "by": {"type": "string"}},
            "required": ["project", "ticket_id"],
        },
    },
    "pm_claim_ticket": {
        "description": "Atomically claim the next matching ticket (-> in-progress). Different agents get different tickets.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "role": {"type": "string", "description": "Discipline that claims the ticket. Known roles: architect, developer, tester, pm, cpp-engineer, graphics-engineer (extensible)."},
                "status": {"enum": ["sprint-backlog"], "description": "Only sprint-backlog tickets are claimable."},
                "by": {"type": "string"},
            },
            "required": ["project", "role"],
        },
    },
    "pm_release_ticket": {
        "description": "Release a ticket back to sprint-backlog (so another agent can pick it up).",
        "inputSchema": {
            "type": "object",
            "properties": {"project": {"type": "string"}, "ticket_id": {"type": "string"}, "by": {"type": "string"}},
            "required": ["project", "ticket_id"],
        },
    },
    "pm_add_comment": {
        "description": "Append a comment to a ticket.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "ticket_id": {"type": "string"},
                "comment": {"type": "string"},
                "by": {"type": "string"},
            },
            "required": ["project", "ticket_id", "comment"],
        },
    },
    "pm_add_artifact": {
        "description": "Record where a produced artifact lives (file/git/path/url/doc) so the next agent can find it.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "ticket_id": {"type": "string"},
                "kind": {"enum": ["file", "git", "path", "url", "doc"]},
                "ref": {"type": "string"},
                "note": {"type": "string"},
                "by": {"type": "string"},
            },
            "required": ["project", "ticket_id", "kind", "ref"],
        },
    },
    "pm_add_todo": {
        "description": "Append a todo (checklist item) to a ticket.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "ticket_id": {"type": "string"},
                "text": {"type": "string"},
                "done": {"type": "boolean"},
                "by": {"type": "string"},
            },
            "required": ["project", "ticket_id", "text"],
        },
    },
    "pm_toggle_todo": {
        "description": "Flip the done state of a ticket's todo by 0-based index.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "ticket_id": {"type": "string"},
                "index": {"type": "integer"},
                "by": {"type": "string"},
            },
            "required": ["project", "ticket_id", "index"],
        },
    },
    "pm_remove_todo": {
        "description": "Remove a ticket's todo by 0-based index.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "ticket_id": {"type": "string"},
                "index": {"type": "integer"},
                "by": {"type": "string"},
            },
            "required": ["project", "ticket_id", "index"],
        },
    },
    "pm_get_backlog": {
        "description": "Prioritized product backlog for a project.",
        "inputSchema": {"type": "object", "properties": {"project": {"type": "string"}}, "required": ["project"]},
    },
    "pm_get_board": {
        "description": "Sprint Kanban grouped by status.",
        "inputSchema": {
            "type": "object",
            "properties": {"project": {"type": "string"}, "sprint": {"type": "string"}},
            "required": ["project"],
        },
    },
    "pm_plan_sprint": {
        "description": "Create/commit a sprint and move given tickets into it.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "project": {"type": "string"},
                "sprint_id": {"type": "string"},
                "ticket_ids": {"type": "array", "items": {"type": "string"}},
                "goal": {"type": "string"},
            },
            "required": ["project"],
        },
    },
    "pm_close_sprint": {
        "description": "Close a sprint; unfinished tickets return to product-backlog.",
        "inputSchema": {
            "type": "object",
            "properties": {"project": {"type": "string"}, "sprint_id": {"type": "string"}},
            "required": ["project", "sprint_id"],
        },
    },
    "pm_audit_traceability": {
        "description": "Build a definition<->verification traceability matrix for a project.",
        "inputSchema": {"type": "object", "properties": {"project": {"type": "string"}}, "required": ["project"]},
    },
}


def _parse_message(line: str) -> Optional[Dict[str, Any]]:
    line = line.strip()
    if not line:
        return None
    try:
        return json.loads(line)
    except json.JSONDecodeError:
        return None


def _handle(msg: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    method = msg.get("method")
    mid = msg.get("id")
    if method == "initialize":
        return {
            "jsonrpc": "2.0", "id": mid,
            "result": {"protocolVersion": "2024-11-05", "capabilities": {"tools": {}}, "serverInfo": {"name": "pm", "version": "1.0"}},
        }
    if method == "notifications/initialized":
        return None
    if method == "tools/list":
        return {
            "jsonrpc": "2.0", "id": mid,
            "result": {"tools": [{"name": n, **TOOL_SCHEMAS[n]} for n in TOOLS]},
        }
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
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=STORE_ROOT, help="data directory for the store")
    args = parser.parse_args()
    global store
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
