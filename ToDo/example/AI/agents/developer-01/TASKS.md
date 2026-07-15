<!-- EXAMPLE (filled) · Agent bundle · Agent-owned · The live board -->

# TASKS — developer-01

> Live board — waiting on the architect's design hand-off to fully start.

## Now

- [ ] Scaffold FastAPI app + Postgres migration harness — started 10:40

## Next

- [ ] Implement `POST /links` (base62 code, collision retry) — acceptance: unit tests pass, matches contract
- [ ] Implement `GET /{code}` 302 redirect + click record — acceptance: redirects, increments count
- [ ] Implement `GET /links/{code}/stats` — acceptance: returns total clicks
- [ ] Smoke test + hand off to tester — acceptance: end-to-end passes locally

## Blocked

- [ ] Start endpoint work in earnest — blocked on: architect milestone 1 (contract not yet approved)

## Done (newest first)

- [x] Repo initialized, CI skeleton added — 10:15
