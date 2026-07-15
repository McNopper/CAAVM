<!-- EXAMPLE (filled) · Mandate · Human-authored · Agents READ-ONLY -->

# BRIEF — the mandate

> Worked example. A real human product owner wrote this before any agent ran.
> The agents (architect, developer, tester) treat it as read-only truth.

## Project

- **Project:** Snip — a URL-shortener REST API
- **Human owner (accountable):** Dana (Product Owner)
- **Last updated (by a human):** 2026-07-14

---

## 1 · Customers & stakeholders (the why)

| Customer / stakeholder | Need or interest | Authority | Constraints they impose |
| --- | --- | --- | --- |
| Marketing team (customer) | Turn long campaign URLs into short, trackable links | sign-off on features | Must ship before Q3 campaign (6 weeks) |
| End users clicking links | Fast, reliable redirects | none (served) | Redirects must never break existing links |
| Security/Compliance | No open redirect abuse, no PII leaks | veto | Must block malicious/looping targets |

- **Final say / escalation target:** Dana (Product Owner)

---

## 2 · Goal (the destination)

> A small, production-ready REST API that shortens URLs, redirects visitors, and
> reports basic click counts — deployed to staging with a runbook.

### Success criteria

- [ ] `POST /links` returns a short code; `GET /{code}` issues a 302 to the target.
- [ ] `GET /links/{code}/stats` returns total click count.
- [ ] p95 redirect latency < 50 ms under 100 req/s.
- [ ] ≥ 90% automated test coverage on core logic; CI pipeline green.
- [ ] Deployed to staging with a one-page runbook.

### Metrics

| Metric | Baseline | Target | How measured |
| --- | --- | --- | --- |
| Redirect p95 latency | n/a | < 50 ms | load test in CI |
| Core test coverage | 0% | ≥ 90% | coverage report |

### Non-negotiables

- Ship to staging within 6 weeks.
- No open-redirect vulnerabilities.
- Stack: Python + FastAPI + PostgreSQL (team standard).

---

## 3 · Scope (the fence)

**In scope:** create short link, redirect, click count, staging deploy, runbook.

**Out of scope:** custom vanity domains, user accounts/auth UI, billing, a web
front-end (API only), production (not just staging) rollout.

**Explicit non-goals:** no analytics dashboard, no A/B testing, no link editing
after creation.

### Autonomy boundary

| Agents may decide alone | Agents must pause for a human |
| --- | --- |
| Library choices within the standard stack, schema details, code structure, test design | Changing the goal/scope, adding a dependency with a restrictive license, anything irreversible (data deletion, deploy to production), spending money |
