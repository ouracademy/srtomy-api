# Technical Plan: Engineering Hardening Backlog

**Branch**: `002-engineering-backlog` | **Date**: 2026-10-16 | **Spec**: [spec.md](./spec.md)

## Summary

Everything from the original P0/P1/P2 audit through auth (PRs #1–#6, #16) is merged to
`main`. This plan tracks what's left, in the order it should be picked up, plus cleanup
requested directly (drop `recurring`, drop App Engine, keep dependencies current).

## Current state check (verified 2026-10-16)

- `spring-boot-starter-parent` is already `4.1.1`, the newest **stable** release (`4.2.0`
  only has milestone builds so far). **No Spring Boot upgrade is actually needed right
  now** — we're already current.
- `spring-cloud-dependencies` (`2025.1.3`) and `spring-cloud-gcp-dependencies` (`8.2.4`)
  are also already at their latest stable versions.
- `springdoc-openapi-starter-webmvc-ui` is pinned at `2.4.0`; `3.1.1` is out (major version
  bump, OpenAPI 3.1 tooling). This is the one real, actionable dependency upgrade.
- Open Dependabot PRs and what to do with each:
  - **#14** springdoc `2.4.0` → `3.1.1` — real, do it (item 3 below).
  - **#15** spring-boot-starter-parent → `4.1.1` — already satisfied by current `pom.xml`;
    close as superseded, don't merge.
  - **#13** appengine-maven-plugin `2.4.4` → `2.8.7` — moot once the plugin is removed
    (item 2); close.
  - **#10** `net.logicsquad:recurring` `0.3` → `0.4` — moot once the dependency is removed
    (item 1); close.
- `grep -rn "net.logicsquad" --include=*.java .` returns **zero** matches outside
  `pom.xml`. The dependency is dead weight — `RecurrentEvent`
  (`src/main/java/org/ouracademy/srtomy/event/RecurrentEvent.java`) is this project's own
  class, not from that library.

## Work items, in order

### 1. Remove `net.logicsquad:recurring` (small, no risk)

Delete the `<dependency>` block from `pom.xml` (currently lines ~81–85). No source changes
needed — confirmed unused above. Verify with `./mvnw -q compile` and a repo-wide grep for
`logicsquad` coming back empty.

### 2. Remove App Engine, standardize on Cloud Run (small, no risk — never deployed there)

- Remove the `appengine-maven-plugin` block from `pom.xml`'s `<build><plugins>`.
- Remove the "### Deploy to appengine" section from `README.md`, keep "### Deploy to
  cloudrun" as the only deploy path documented.
- Close Dependabot PR #13 (appengine bump) as no longer applicable.

### 3. Upgrade springdoc-openapi 2.4.0 → 3.1.1

Real upgrade, not just a version bump — springdoc 3.x targets OpenAPI 3.1 and
Spring Boot 4/Framework 7 more directly. Bump the version in `pom.xml`, then verify:
- `./mvnw -q -B verify` green.
- `/v3/api-docs` and `/swagger-ui/index.html` still render locally
  (`spring-boot:run -Dspring-boot.run.profiles=local`).
- Check springdoc's own migration notes for breaking annotation changes if `verify` fails
  (last time we touched this area, nothing in this codebase used deprecated springdoc
  annotations, so this is expected to be low-risk).

Once merged, close Dependabot PR #15 (already-satisfied parent bump) as superseded.

### 4. SonarCloud CSRF Security Hotspot (manual, not code)

`SecurityConfig.filterChain()`'s `csrf.disable()` is flagged as a Security Hotspot on PR
#16's SonarCloud Automatic Analysis. This is **correctly** disabled (the API is a fully
stateless Bearer-token resource server — see the justification comment already in
`SecurityConfig.java`), and route-scoping it (`ignoringRequestMatchers`) would not fix
anything: that's literally the rule's own "Noncompliant" example, not its fix. The actual
action needed: a human with SonarCloud access reviews the hotspot at
Issues → Security Hotspots → that line → **Review → Safe**, citing the existing code
comment. No further code change here.

### 5. API versioning — `/v1` (Stage G, coordinated with srtomy-ui)

- `server.servlet.context-path: /v1` in `application.yml`.
- Keep `/actuator/**` off `/v1` (separate management port or
  `management.endpoints.web.base-path`) so k8s/Cloud Run health probes don't need to
  change.
- `srtomy-ui`: update `NEXT_PUBLIC_API_URL` to include `/v1`.
- Merge both PRs together — this is a breaking path change, there's no good way to stage
  it independently without a window of 404s.

### 6. Cloud Run deploy pipeline (Stage H)

A `deploy` job in CI, triggered on merge to `main` with green checks, running the
`spring-boot:build-image` + `docker push` + Cloud Run deploy flow already documented in
`README.md`, authenticated via GCP Workload Identity Federation
(`google-github-actions/auth`) — no long-lived JSON key. **Needs the user to create the
WIF pool/provider/service account in GCP first**; give exact `gcloud` commands when this
item is picked up, same pattern as `SONAR_TOKEN`.

### 7. Observability — structured logging (Stage I)

Add `spring-cloud-gcp-starter-logging` for JSON-structured logs that Cloud
Logging/Error Reporting parse automatically on Cloud Run, no extra agent. Minimal logback
config for the expected format. Low effort, do after the deploy pipeline exists (no point
shipping structured logs nowhere).

## Also worth doing (not yet staged as its own PR — flagging so it doesn't get lost)

- **`cors_origin: "*"`** in both `application.yml` and `application-prod.yml` is wide open.
  Now that the API requires auth, a stolen/leaked token from a malicious origin is a more
  realistic risk than it was pre-auth. Scope it to the actual srtomy-ui origin(s) once a
  real domain exists.
- **`hikari.maximumPoolSize: 2`** in `application-prod.yml` — inherited from an old
  ElephantSQL free-tier limit, already flagged with a comment, never revisited against
  whatever Postgres is actually in front of Cloud Run. Worth confirming real limits before
  Stage 6 (Cloud Run deploy) goes live — a pool too large for the DB tier will just produce
  connection errors under load.
- **`osv-scanner --no-resolve`** in CI was a workaround for scanning a SNAPSHOT parent.
  The parent has been GA (`4.1.1`) since PR #3 — worth trying the flag's removal now to get
  full dependency resolution in the scan instead of the degraded mode.
- **`Complaint.dni`** has no `@NotBlank`/`@NotNull` (only `@Pattern`, which a `null` value
  satisfies) but the DB column is `NOT NULL` — a real request omitting it 500s instead of
  400ing. Documented in `ComplaintControllerTests` as a known gap; fix is a one-line
  `@NotBlank` addition plus a test, small enough to bundle into item 1 or 2's PR if
  convenient, otherwise its own tiny PR.
- **Per-role authorization**: still flat "authenticated = full access" (confirmed
  acceptable for now in the original auth design). The data model (`AccountabilityType`,
  roles sync) already supports real roles — this is a deliberate scope cut, not an
  oversight, but worth revisiting once there's more than one kind of user hitting the API.

## Verification

Each item: `./mvnw -q -B verify` green, `python3 scripts/check-language.py` clean, small
commit, own PR — same convention as the rest of this repo (see `AGENTS.md`). Items 1–4 are
independent and can happen in any order; item 5 (`/v1`) should land before items 6–7 since
it changes the base URL everything else builds against.
