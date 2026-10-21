# Agent instructions

@README.md

## Commits

Always use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages:
`<type>(<optional scope>): <description>`, allowed types: feat, fix, docs, style, refactor, perf, test, chore, ci, build, revert.
Enforced by `.githooks/commit-msg` (`git config core.hooksPath .githooks`) and by CI.

## Code style

Java code is formatted with Spotless (`google-java-format`). It runs automatically on edit via the Claude Code hook; CI verifies it with `./mvnw spotless:check`.

### Entity ids

Every JPA entity has its own typed id class (a `record` implementing `EntityId`, e.g. `PartyId`, `EventId`), not a raw `UUID`/`Long` and not a shared generic id reused across unrelated entities. The id class lives next to its entity (same package), not in a shared/common package. See `org.ouracademy.srtomy.person.PartyId` (and its entity `Party`) for the pattern, and `org.ouracademy.srtomy.domain` for the shared plumbing (`EntityId`, `IdGenerator`, `EntityIdJavaType`, `EntityIdTypeContributor`) every id type relies on. All ids are UUIDv7, generated in Java via a `@Id XxxId id = XxxId.generate();` field initializer — no `@GeneratedValue`, no `AttributeConverter` (Hibernate rejects those on `@Id` fields).

## Language policy

All code must be in English: file names, classes, methods, variables, comments. The only exception is the content of customer-facing strings (validation/error messages shown to the end user), which may be in Spanish. Checked in CI with `scripts/check-language.py`.

## Specs and plans

Use spec-kit (the `speckit-*` skills) for specs, plans and task lists. Specs live under `specs/<NNN-feature-name>/`. Do not use addy's `/spec` or `/planning` commands, the `spec-driven-development` and `planning-and-task-breakdown` skills, or write `SPEC.md` or `tasks/plan.md` at the repo root. Those write to a second place and split the plan.

Other addy skills stay available for code review, TDD, security and performance.
