# Agent instructions

@README.md
@docs/code-style.md

## Commits

Always use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages:
`<type>(<optional scope>): <description>`, allowed types: feat, fix, docs, style, refactor, perf, test, chore, ci, build, revert.
Enforced by `.githooks/commit-msg` (`git config core.hooksPath .githooks`) and by CI.

## Code style

Java code is formatted with Spotless (`google-java-format`). It runs automatically on edit via the Claude Code hook; CI verifies it with `./mvnw spotless:check`.

The project is organized into domain modules (`party`, `person`, `organization`,
`accountability`, `event`, `attendance`, `complaint`, `user`, `iam`, plus the non-domain
`shared`), each with its own `domain/command/query/repository|gateway/controller`
structure, naming conventions (`Command`/`Query`/`Handler`/`handle`/`Result`), and
cross-module communication rules (including the entity-id convention). All of this
is documented in [docs/code-style.md](./docs/code-style.md) and enforced by
`src/test/java/org/ouracademy/srtomy/ArchitectureTests.java` (ArchUnit), which runs
as part of `./mvnw test`.

## Language policy

All code must be in English: file names, classes, methods, variables, comments. The only exception is the content of customer-facing strings (validation/error messages shown to the end user), which may be in Spanish. Checked in CI with `scripts/check-language.py`.

## Specs and plans

Use spec-kit (the `speckit-*` skills) for specs, plans and task lists. Specs live under `specs/<NNN-feature-name>/`. Do not use addy's `/spec` or `/planning` commands, the `spec-driven-development` and `planning-and-task-breakdown` skills, or write `SPEC.md` or `tasks/plan.md` at the repo root. Those write to a second place and split the plan.

Other addy skills stay available for code review, TDD, security and performance.
