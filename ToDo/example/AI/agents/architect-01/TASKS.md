<!-- EXAMPLE (filled) · Agent bundle · Agent-owned · The live board -->

# TASKS — architect

> Live board. This is what Dana scans to see what the architect is doing right now.

## Now

- [ ] Draft ADR-002: open-redirect defense (allow-list vs deny-list) — started 10:20

## Next

- [ ] Finalize `openapi.yaml` for `/links`, `/{code}`, `/links/{code}/stats` — acceptance: validates against OpenAPI 3.1
- [ ] Define Postgres schema (`links`, `clicks`) — acceptance: supports click count query < 5ms
- [ ] Request design review from Dana — acceptance: review passed

## Blocked

- [ ] ADR-002 finalize — blocked on: D-1 (which target domains are allowed?)

## Done (newest first)

- [x] ADR-001: short-code = base62, 7 chars, collision-retry — recorded — 09:55
- [x] Drafted endpoint list + status codes (302 for redirect) — 09:30
