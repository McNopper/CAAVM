<!-- EXAMPLE (filled) · Agent bundle · Agent-owned · The live board -->

# TASKS — tester

> Live board — preparing the harness while the developer builds.

## Now

- [ ] Write the criterion→check matrix into an executable test plan — started 10:50

## Next

- [ ] Contract tests generated from `openapi.yaml` — acceptance: fail on any drift
- [ ] Integration tests: create → redirect → stats — acceptance: cover all 3 endpoints
- [ ] Load test scenario @ 100 rps with p95 gate — acceptance: CI fails if p95 ≥ 50ms
- [ ] Security tests: open-redirect rejection — acceptance: blocks disallowed targets

## Blocked

- [ ] Run integration + load tests — blocked on: developer milestone 2 (service not built yet)
- [ ] Finalize security tests — blocked on: D-1 (allow-list definition)

## Done (newest first)

- [x] Chose load-test tool + coverage gate config — 10:35
