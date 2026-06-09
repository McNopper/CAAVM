# OUTPUT — current state (process-only, auto-generated)

> **This file belongs to the process — do not edit it by hand.** Hephaestus overwrites it
> (Intake seeds it, Report refreshes it after each increment) with everything it
> understood, derived, created, and updated. You write your ideas in
> [`INPUT.md`](INPUT.md); the process never edits that file, only reads it.

_Last run: not yet run._

## Loop & maturity

<!-- Which loop this is and the maturity rung it ran. Maturity is tracked PER SLICE — each
     vertical slice climbs its own mvp → harden → complete ladder; a run advances every ready slice. -->

- loop: — · this run's level (default): — · next run's level: — · INPUT.md fully resolved: —

## Captured from `INPUT.md`

<!-- Intake records here what it read from INPUT.md and what it did with it. -->

| Input (short) | Mapped to | Result |
|---------------|-----------|--------|
| _nothing captured yet_ | — | — |

## Resolution of `INPUT.md`

<!-- How much of INPUT.md is resolved across loops. Status: resolved | partial | queued. -->

| Input idea | Status | Note |
|------------|--------|------|
| _nothing captured yet_ | — | — |

## Resolved configuration (defaults + INPUT overrides)

<!-- The effective settings actually used this run. -->

- language: — · build: — · tests: — · docs toggle: — · strategy: — · per-phase models: —

## Increments

<!-- One line per backlog item (vertical slice). Example of the shape Report maintains: -->

- [ ] **INC-001** — _title_ — `queued` — rung reached: —
  - stage reached: — · gate: — · debt / deferred to next loop: —
  - walking-skeleton slice: — · hierarchy: — deployable(s) ◄ — module(s) ◄ — component(s) ◄ — unit(s) · topology: —

## Per-slice maturity

<!-- Each increment is a vertical slice climbing its OWN ladder. A run advances every ready slice. -->

| Increment | Walking-skeleton slice | Current rung | Next rung |
|-----------|------------------------|--------------|-----------|
| —         | —                      | —            | —         |

## Provisional vs stable contracts

<!-- Architecture/interfaces start PROVISIONAL (a hypothesis); the Adaptation step promotes the ones
     the running code validated to STABLE. Listed per latest increment. -->

| Contract | State | Evidence (test that validated it) |
|----------|-------|-----------------------------------|
| —        | provisional / stable | — |

## Assumption / stub debt

<!-- Every provisional contract, stub, or driver mocked at a slice boundary is first-class debt with
     an owner and a retirement condition; retired as soon as real code replaces it. -->

| Item | Owner | Retire when | Status |
|------|-------|-------------|--------|
| —    | —     | —           | open / retired |

## Composition hierarchy (latest increment)

<!-- system → software(executable) → module → component → unit. A component shared across modules is listed once. -->

| deployable (executable) | module | components | units |
|-------------------------|--------|-----------|-------|
| —                       | —      | —         | —     |

## Test levels (latest increment)

<!-- Each level fans out per component / per module / per deployable; a red level triggers a targeted repair of the failing element. -->

| unit (6) | component (7) | module (8) | system (9) | acceptance (10) |
|----------|---------------|------------|------------|-----------------|
| —        | —             | —          | —          | —               |

## Next action

- _Run the workflow / process to populate this report._

---

_Legend: `queued` → not started · `in-progress` → mid V-pass · `passed` → gate met for
the current rung · `failed` → gate not met (re-looping). Checkbox ticked = increment passed
its gate at its current rung. Each increment is built as a **walking skeleton** (thinnest end-to-end
vertical slice) first, then deepened. **Re-run the workflow to climb the ladder per slice
(mvp → harden → complete) until `INPUT.md` is fully resolved.**_
