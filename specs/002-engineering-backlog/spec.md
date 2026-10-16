# Feature Specification: Engineering Hardening Backlog

**Feature Branch**: `002-engineering-backlog`

**Created**: 2026-10-16

**Status**: Draft

**Input**: Running list of best-practices work identified after the P0/P1/P2 security,
testing, resilience, and Spring Boot 4 effort (PRs #1–#6, #16, all merged to `main`).
Tracks what's left, plus cleanup requested directly: drop `net.logicsquad:recurring`,
drop App Engine support in favor of Cloud Run only, and keep Spring Boot/dependencies
on their latest stable versions so Dependabot PRs can close instead of piling up.

## Why

The initial hardening push (auth, tests, resilience, exception handling, Boot 4 upgrade)
is done and merged. What remains is lower-urgency but still real: API versioning, a real
deploy pipeline, observability, and some dependency/infra cleanup that's accumulated
Dependabot noise and dead config (App Engine was never actually used after the project
settled on Cloud Run).

## Scope

See `plan.md` in this directory for the technical breakdown, current state of each item,
and recommended order. This spec intentionally covers multiple unrelated small efforts
(not one coherent user-facing feature) because they were identified together during the
same audit — each is still delivered as its own small PR per the repo's commit/PR
convention (see `AGENTS.md`).

## Out of scope

- Per-role/fine-grained authorization (currently: authenticated = full access, confirmed
  acceptable for now).
- Rate limiting in application code (infra/gateway concern, not this app).
- CODEOWNERS (explicitly declined earlier).
