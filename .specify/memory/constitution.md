# Sr. Tommy API Constitution

## Core Principles

### I. Quality Gates Are Never Weakened

Every change MUST pass `./mvnw verify` (Spotless format check, JaCoCo coverage check, full test
suite) before it is considered done, and CI MUST enforce the same gate. Changes MUST NOT add
`@Disabled`, lowered JaCoCo thresholds, `--no-verify` commits, or suppressed Sonar findings to make
a check pass without a documented reason. The JaCoCo coverage floor only ever moves up.
Rationale: gates only protect the codebase if they cannot be bypassed one exception at a time.

### II. Test at the Right Level

Pure logic (validators, request DTOs, service classes with mocked collaborators) MUST have plain
unit tests. Controllers MUST be tested with `@WebMvcTest` + `MockMvcTester` and mocked
repositories/services, not a full `@SpringBootTest` against a real database, unless the behavior
under test genuinely requires the full context (e.g. security filter chain end-to-end, Flyway
migrations). Test classes MUST follow the naming convention: unit and slice tests end in `Tests`
(e.g. `OrganizationControllerTests`); tests that load the full Spring context end in
`IntegrationTests` (e.g. `SecurityConfigIntegrationTests`). A bug fix MUST include a test that fails
without the fix.
Rationale: full-context tests are slow and couple unrelated concerns together; the suffix makes the
cost of a test class visible without opening it.

### III. Security by Default

Every endpoint requires a valid Auth0 JWT except `/actuator/health/**`, which stays public for
container health probes. CSRF protection may stay disabled only because this API is a fully
stateless, Bearer-token-only resource server (`SessionCreationPolicy.STATELESS`, no cookies); if
that assumption ever changes, CSRF protection MUST be reconsidered, not silently left off. Secrets
(DB credentials, Auth0 client secret) MUST come from GCP Secret Manager (`sm://`), never be
committed, and local/test configuration MUST use disposable or fake credentials only. New
dependencies and CI steps MUST NOT weaken `gitleaks`/`osv-scanner` scanning in `ci.yml`.
Rationale: the app holds personal data of church members, including minors.

### IV. English Code, Spanish Only for End Users

File names, classes, methods, variables, and comments MUST be in English. The only exception is the
content of customer-facing strings (validation/error messages shown to the end user), which may be
in Spanish. Enforced in CI by `scripts/check-language.py`.
Rationale: consistent with `AGENTS.md`; keeps the codebase navigable regardless of contributor
language.

### V. Simplicity and Deliberate Dependencies

Prefer what Spring/the JDK already provide (e.g. `ProblemDetail` for RFC 9457 errors) over adding a
third-party library, especially one in maintenance mode or with unclear support for the current
Spring Boot major version. A new dependency MUST be justified in its PR. A dependency with zero
remaining usages MUST be removed, not left "just in case" (see: `net.logicsquad:recurring` removal).
Deploys target Cloud Run only — no App Engine-specific tooling.
Rationale: every dependency is maintenance burden and attack surface; unused or legacy ones compound
upgrade cost, as the Zalando `problem-spring-web` and App Engine history both showed.

### VI. Small, Traceable Changes

Work happens on a branch and reaches `main` through a pull request. Commits MUST follow Conventional
Commits (`<type>(<scope>): <description>`), enforced by `.githooks/commit-msg` and CI. Each
logical change (a rename, a dependency bump, a behavior change) MUST be its own small commit rather
than one large one. Non-trivial work (more than a one-line fix) MUST go through spec-kit (`specs/`)
for its spec and technical plan rather than ad-hoc notes or comments scattered across files.
Rationale: small, reviewed, well-described changes are easier to verify, revert, and explain later.

## Technical Constraints

- Runtime: Java 21 via sdkman (`sdk install/use java 21.0.0.0-amzn`); Spring Boot 4.x; Postgres
  >= 14 (Flyway/Hibernate dropped older support) — currently `postgres:16-alpine` in
  `docker-compose.yml` and CI.
- Database schema changes MUST go through Flyway migrations (`src/main/resources/db/migration`),
  never manual schema edits.
- Formatting: Spotless with `google-java-format`, applied automatically on edit via the Claude Code
  hook, enforced in CI with `./mvnw spotless:check`.
- Static analysis: SonarCloud (`ouracademy_srtomy-api`) runs as a CI job and as an independent
  Automatic Analysis check; Security Hotspots require a human "Safe"/"Fixed" review in the SonarCloud
  dashboard — a code comment alone does not resolve one.
- Dependency hygiene: Dependabot (weekly, Maven + GitHub Actions) plus `osv-scanner` and `gitleaks`
  in CI, scoped to the PR/push commit range.

## Development Workflow

- Local setup, GCP auth, and run instructions: see `README.md`.
- Enable repo git hooks once per clone: `git config core.hooksPath .githooks`.
- Before opening a PR: `./mvnw -q -B verify` passes locally against the `docker-compose` Postgres.
- Specs and technical plans for non-trivial work live under `specs/<NNN-feature-name>/` via
  spec-kit's `speckit-*` skills (see `AGENTS.md`'s "Specs and plans" section).
- Reviewers MUST verify compliance with this constitution; deviations MUST be justified in the PR.

## Governance

This constitution supersedes other project practices where they conflict; `AGENTS.md` and
`README.md` provide runtime guidance and MUST stay consistent with it. Amendments are made by a
pull request that updates this file, states the reason, and, for breaking changes, a migration
plan. Versioning follows semantic versioning: MAJOR for removed or redefined principles, MINOR for
added principles or materially expanded guidance, PATCH for clarifications. Compliance is reviewed
on every PR and revisited whenever a retrospective or incident shows a principle is missing or
wrong.

**Version**: 1.0.0 | **Ratified**: 2026-10-17 | **Last Amended**: 2026-10-17
