# Agent instructions

@README.md
@docs/code-style.md

## Commits

Always use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages:
`<type>(<optional scope>): <description>`, allowed types: feat, fix, docs, style, refactor, perf, test, chore, ci, build, revert.
Enforced by `.githooks/commit-msg` (`git config core.hooksPath .githooks`) and by CI.

## Dependencies

The product owner wants the latest versions at all times. When a dependency problem shows up (the `secrets and dependencies` check, Dependabot or a CVE report a vulnerability, a deprecation, a failing check caused by an old library), do not patch around it by pinning or excluding only the vulnerable artifact: update the dependency and its parents (Spring Boot, plugins, the Maven wrapper) to the latest version, major ones included, and fix what breaks. Check `./mvnw versions:display-dependency-updates` and update the rest while you are at it. Pin or override a version only as a last resort when no released parent allows the fix, and write it in the technical debt table (see below) with when to remove it.

## Code style

Java code is formatted with Spotless (`google-java-format`). It runs automatically on edit via the Claude Code hook; CI verifies it with `./mvnw spotless:check`.

The project is organized into domain modules (`party`, `person`, `organization`,
`accountability`, `event`, `attendance`, `complaint`, `user`, `iam`, plus the non-domain
`shared`), each with its own `domain/command/query/repository|gateway/controller`
structure, naming conventions (`Command`/`Query`/`Handler`/`handle`/`Result`), and
cross-module communication rules (including the entity-id convention). All of this
is documented in [docs/code-style.md](./docs/code-style.md) and enforced by
`src/test/java/org/ouracademy/ovejafiel/ArchitectureTests.java` (ArchUnit), which runs
as part of `./mvnw test`.

## Scheduled work and external systems

- **Never use `@Scheduled` or `@EnableScheduling`; use Cloud Scheduler (work on a calendar) or Cloud Tasks (work that must happen later or be retried).** Both call an internal endpoint under `/v1/internal/**`, which only the scheduler's service account may call (see `InternalSecurityConfig`). A calendar job is one `google_cloud_scheduler_job` in `infra/terraform/modules/scheduler`; a queue is in `infra/terraform/modules/tasks`. `ArchitectureTests` fails the build if `@Scheduled` appears.
- **Anything done outside our database (an email, a WhatsApp message, a webhook, another service) goes through the outbox**, never inside the transaction that asked for it: enqueue with `EnqueueMessageCommandHandler` in the same transaction (it also schedules the delivery task, and if that fails the whole transaction rolls back), and implement an `OutboxMessageHandler` for the channel that is safe to repeat. Details in `docs/code-style.md`, "Trabajo programado y sistemas externos".

## Language policy

All code must be in English: file names, classes, methods, variables, comments. The only exception is the content of customer-facing strings (validation/error messages shown to the end user), which may be in Spanish. Checked in CI with `scripts/check-language.py`.

## Pending work and cleanup

Anything left to do or to clean up (unused secrets, dead code, legacy names...) goes into the technical debt table of `docs/redesign.md` (section 10, "009"). Ask the product owner before doing any item from that list, especially cleanups; never remove things on your own initiative.

## Specs and plans

Use spec-kit (the `speckit-*` skills) for specs, plans and task lists. Specs live under `specs/<NNN-feature-name>/`. Do not use addy's `/spec` or `/planning` commands, the `spec-driven-development` and `planning-and-task-breakdown` skills, or write `SPEC.md` or `tasks/plan.md` at the repo root. Those write to a second place and split the plan.

Other addy skills stay available for code review, TDD, security and performance.
