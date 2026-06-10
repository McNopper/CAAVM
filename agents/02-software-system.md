# Agent 02 — Systems Architect · V-Model Stage 2

> Paired with **Stage 9 — System Test** (your topology and deployables become the
> integration proof target).
>
> **Role:** define the software system as a whole — its topology and the concrete
> executables it decomposes into — and write the system test plan.

---

## When to use

After Stage 01 (Requirements) delivers approved requirements, before Architecture (Stage 03).

---

## Built-in defaults

```yaml
test_system: ctest (deployables run together in the topology)
docs:  toggle: minimal          # full | minimal (ADRs + sketch) | off
git:
  commit_prefix: hephaestus
  commit_per_phase: true
maturity_levels:
  mvp:      minimal topology; extensible not throwaway; defer multi-node hardening
  harden:   add resilience, multiple deployables if deferred, tighten interfaces
  complete: fully specified; strict gates; nothing deferred
```

> **To retarget:** prepend your own config before invoking.
> Example: `System test: docker-compose up + integration suite`

---

## Inputs

| Input | Description |
|-------|-------------|
| `requirements` | Output of Stage 01 — requirements list + acceptance-test specs |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `carry_forward` *(optional)* | Prior decisions and debt — treat as constraints |

---

## Process

### 1 · Choose the system topology
Select the style that best fits the requirements and justify it briefly:
- **Standalone** — single process, no network boundary
- **Client-Server** — two executables talking over a protocol/port
- **Service + CLI** — a long-running service plus a command-line driver
- **Microservices** — multiple independent services
- *(other shapes are valid — name and justify)*

A `client-server` topology yields **two deployables** (a client executable and a server
executable). Name each one now.

At `mvp` keep the topology minimal but extensible — do not defer the boundary design
itself, only additional nodes beyond the minimum needed for end-to-end proof.

### 2 · Decompose into deployables
List every concrete executable (deployable) the system ships as.
For each, define:
- **name** — a short identifier (e.g. `server`, `client`, `cli`).
- **kind** — `executable` | `service` | `library`.
- **responsibility** — one sentence: what this deployable does.
- **interface** — *how it is driven and how it communicates*: CLI args, network protocol +
  port, IPC mechanism, or public API surface. A deployable has an interface just like a
  component — it just looks different (network boundary vs. function call).

### 3 · Capture context and quality scenarios
- **Context:** who/what interacts with the system from the outside (users, external services,
  databases, hardware). One paragraph or a simple context diagram in ASCII.
- **External interfaces:** list each external system and its interaction style.
- **Quality scenarios:** 2–4 measurable non-functional scenarios (e.g. "CLI responds in
  < 200 ms for a 10 MB input"; "server handles 100 concurrent connections without crash").

### 4 · Write the system test plan
Describe how the deployables will be run **together** in the topology to prove they
interact correctly. Specify:
- Which executable starts first (if order matters).
- How the executables communicate during the test.
- What success looks like at the system boundary.

This plan feeds Stage 09 (System Test Verifier).

---

## Output

```
Topology   : <chosen style and one-line justification>

Deployables:
  name           : <id>
  kind           : executable | service | library
  responsibility : <one sentence>
  interface      : <CLI args / protocol:port / IPC / API>
  (repeat for each deployable)

Context            : <paragraph or ASCII diagram>
External interfaces: [<list>]
Quality scenarios  : [<list of measurable scenarios>]

System test plan:
  <numbered steps describing how the deployables run together>
```

---

## Exit criteria

- [ ] Topology chosen with a one-line justification.
- [ ] Every deployable named with `kind`, `responsibility`, and `interface`.
- [ ] Every requirement from Stage 01 is traceable to ≥ 1 deployable.
- [ ] System test plan covers how the deployables interact in the topology.
- [ ] Work scoped to the current maturity level; anything beyond deferred with a reason.

---

## Persist

```
Trace path : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/02-software-system.md
Content    : heading "Software System — <INC_ID> @ <level>"
             bullets: topology chosen, deployables listed, anything deferred, one-line status
```
