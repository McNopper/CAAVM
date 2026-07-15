<!-- EXAMPLE (filled) · AI · PM INBOX · worker → PM bubble-up -->

# INBOX — issues bubbling up to the PM

> Worked example. Worker instances raise issues here; the PM triages each one and
> either resolves it or escalates to `../../../Human/DECISIONS.md`.

## Legend

- **Kind:** `question` · `blocker` · `decision` · `risk` · `fyi`
- **State:** `open` · `pm-resolved` · `escalated`

---

## Open (newest first)

_(none — the one open item was escalated, see below)_

---

## PM-resolved (newest first)

### I-2 · Two developers for the build?
- **Raised by:** PM's own planning at 2026-07-14 09:40
- **Resolved by PM:** 09:45 — split the build into two lanes and spawned
  **developer-01** (write path) and **developer-02** (read path) so create and
  redirect proceed in parallel. Reflected in the roadmap + board.

### I-3 · Shared schema ownership between developer-01 and developer-02
- **Raised by:** developer-02 at 11:05
- **Resolved by PM:** 11:10 — developer-01 owns the schema of record; developer-02
  consumes it read-only. No escalation needed.

## Escalated (newest first)

### I-1 → Human/DECISIONS.md D-1
- **Raised by:** architect-01 at 10:22
- **Escalated:** 10:25 — reason: choosing Snip's open-redirect security posture
  crosses the `../../../Human/BRIEF.md` autonomy boundary. Sent to Dana with the PM's
  recommendation (hybrid). Blocks architect-01 (ADR-002) and tester-01 (security suite).
