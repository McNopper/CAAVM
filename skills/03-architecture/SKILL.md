---
name: 03-architecture
description: Hephaestus V-Model Stage 3. Decompose each executable into modules with clear boundaries (Ports and Adapters). Use after the software system topology is approved. Paired with Stage 8 module testing. Executes phase logic only - no version control.
---

# Skill 03 — Architect · V-Model Stage 3

> Paired with **Stage 8 — Module Test** (your module interfaces are the mocked-boundary
> contracts that Stage 08 verifies).
>
> **Role:** architect *each deployable independently* — choose its pattern, decompose it
> into modules, define module interfaces, write ADRs, and emit the component work-list
> that seeds Design (Stage 04). All contracts are **PROVISIONAL** (a hypothesis).

---

## When to use

After Stage 02 (Software System) delivers approved topology and deployables,
before Design (Stage 04).

---

## Built-in defaults

```yaml
language: C++23
compilers: [clang++ ≥ 17, g++ ≥ 13]
build: CMake ≥ 3.28 + Ninja
packages: vcpkg
module_packaging: static       # default per module: static | shared/DLL | header-only
test_module: ctest             # components compose into the module; other modules mocked
docs:
  toggle: minimal              # full (arc42 + UML) | minimal (ADRs + sketch) | off
  template: arc42
  sections:
    [Introduction and Goals, Constraints, Context and Scope, Solution Strategy,
     Building Block View, Runtime View, Deployment View, Crosscutting Concepts,
     Architecture Decisions, Quality Requirements, Risks and Technical Debt, Glossary]
architecture_patterns:
  catalog:
    [Layered (N-Tier), Client-Server, Microservices, Event-Driven, MVC,
     Service-Oriented (SOA), Repository, CQRS, Domain-Driven Design (DDD), Peer-to-Peer]
clean_code:
  architecture: Ports & Adapters (Hexagonal) — domain core has no I/O dependencies
  dependency_rule: source dependencies point inward, toward the domain
maturity_levels:
  mvp:      minimal modules; each extensible not throwaway
  harden:   add modules deferred at mvp; tighten boundaries
  complete: fully specified; strict gates; nothing deferred
```

> **To retarget:** prepend your own config before invoking.

---

## Inputs

| Input | Description |
|-------|-------------|
| `requirements` | Stage 01 output — REQ list |
| `software_system` | Stage 02 output — topology + deployables |
| `maturity_level` | `mvp` \| `harden` \| `complete` |
| `carry_forward` *(optional)* | Prior ADRs and debt — treat as constraints |

---

## Process

### 1 · Architect each deployable independently
For **every deployable** from Stage 02:
- Pick one pattern from the catalog above that fits its responsibility.
- Justify the choice in ≤ 2 sentences (record as an ADR).
- Example: a client might be **Layered with 3 modules**; the server **Repository** —
  each deployable gets its own pattern.

### 2 · Decompose each deployable into modules
A module is the largest independently-testable building block inside a deployable.
For each module, define:
- **name**, **deployable** (parent executable), **responsibility** (one sentence).
- **interfaces** — contracts this module exposes to peer modules.
- **packaging** — `static` (default) | `shared`/`DLL` | `header-only`.

Apply the clean-code rules: Ports & Adapters, inward dependency rule.
Mark every module interface contract **`[provisional]`**.

At `mvp` keep the count small — the thinnest decomposition that still crosses every
tier end-to-end. Defer extra modules with a reason.

### 3 · Write ADRs
One ADR per significant decision (pattern choice, module boundary, packaging, dependency
rule exception):

```
# ADR-<n>: <title>   [state: provisional]
Context     : <forces and constraints>
Decision    : <what was chosen>
Consequences: <trade-offs>
Evidence    : n/a (provisional — updated at Adaptation in Stage 10)
```

### 4 · Document (respect the docs toggle)
- **full:** arc42 sections relevant to this stage; mark irrelevant sections *n/a*.
- **minimal:** ADRs + a brief building-block sketch (ASCII box diagram is fine).
- **off:** ADRs only (one-line rationale per irreversible decision).

### 5 · Produce the module test plan
Describe how each module's components will be assembled and tested in isolation,
with other modules mocked at the boundary. This feeds Stage 08.

### 6 · Produce the component work-list (seeds Stage 04)
One row per component:
- **name**, **modules[]** (one or more — a shared component listed once, not duplicated),
  **responsibility** (one line).

Design (Stage 04) details each component's interface and units.

### 7 · Map requirements to modules
For every `REQ-` ID from Stage 01, name the module(s) that implement it.

---

## Output

```
Deployable patterns:
  deployable   : <name>
  pattern      : <chosen pattern>
  justification: <≤ 2 sentences>

Modules:
  name         : <id>
  deployable   : <parent executable>
  responsibility: <one sentence>
  interfaces   : [<contract names/types>]
  packaging    : static | shared | header-only
  state        : provisional

ADRs: (one block per decision — see template above)

Module test plan:
  <per module: components compose + what is mocked at the boundary>

Component work-list:
  name         : <component id>
  modules      : [<module ids>]
  responsibility: <one line>

REQ-to-module map:
  REQ-<id> → <module(s)>
```

---

## Exit criteria

- [ ] Every deployable has a named architecture pattern with a justification ADR.
- [ ] Every module maps to exactly one deployable; has `responsibility` and `interfaces`.
- [ ] Every requirement maps to ≥ 1 module.
- [ ] All contracts and ADRs tagged `[provisional]`.
- [ ] Module test plan present.
- [ ] Component work-list covers every module with ≥ 1 component.
- [ ] Work scoped to maturity level; extras deferred with reason.

---

## Persist

```
Trace path : docs/hephaestus/trace/<INC_ID>/loop<N>-<level>/03-architecture.md
Content    : heading "Architecture — <INC_ID> @ <level>"
             bullets: patterns chosen, module count, anything deferred, one-line status
```
