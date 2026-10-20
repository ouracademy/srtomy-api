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

### 1. ✅ DONE — Remove `net.logicsquad:recurring`

[#19](https://github.com/ouracademy/srtomy-api/pull/19), merged. Confirmed unused, deleted
with no source changes.

### 2. ✅ DONE — Remove App Engine, standardize on Cloud Run

[#19](https://github.com/ouracademy/srtomy-api/pull/19), merged — same PR as item 1.
Removed `appengine-maven-plugin`, `src/main/appengine/app.yaml`, and the README section.
Dependabot PR #13 closed as no longer applicable.

### 3. ✅ DONE — Upgrade springdoc-openapi 2.4.0 → 3.1.1

[#20](https://github.com/ouracademy/srtomy-api/pull/20), merged. No source changes needed.
Dependabot PR #15 (already-satisfied parent bump) closed as superseded.

**Follow-up dependency audit** also found `google-java-format` still pinned at `1.28.0`
for a JVM 17 constraint that stopped applying once this project moved to Java 21 — bumped
to `1.36.0` in [#22](https://github.com/ouracademy/srtomy-api/pull/22), merged (`1.37.0`,
the actual latest, fails under the current spotless-maven-plugin — see that PR). Everything
else in `pom.xml` (Spring Boot, spring-cloud, spring-cloud-gcp, hypersistence-utils,
jacoco-maven-plugin, sonar-maven-plugin, spotless-maven-plugin) was already at its latest
stable release as of this audit.

### 4. ✅ DONE — SonarCloud CSRF Security Hotspot

`SecurityConfig.filterChain()`'s `csrf.disable()` is flagged as a Security Hotspot on PR
#16's SonarCloud Automatic Analysis. This is **correctly** disabled (the API is a fully
stateless Bearer-token resource server — see the justification comment already in
`SecurityConfig.java`), and route-scoping it (`ignoringRequestMatchers`) would not fix
anything: that's literally the rule's own "Noncompliant" example, not its fix. **Done by
the user** via Issues → Security Hotspots → that line → Review → Safe.

### 5. ✅ DONE — API versioning `/v1` (Stage G)

[#23](https://github.com/ouracademy/srtomy-api/pull/23) +
[srtomy-ui#43](https://github.com/ouracademy/srtomy-ui/pull/43). Both merged.

Implemented via `WebMvcConfigurer.configurePathMatch` prefixing only `@RestController`
classes with `/v1` — not `server.servlet.context-path` and not a separate management port,
so `/actuator/**` stays unversioned (it isn't `@RestController`-registered) and the app
still runs on the single port Cloud Run exposes. `srtomy-ui`'s `NEXT_PUBLIC_API_URL` stays
origin-only; `/v1` is appended once in `app/(api)/api.tsx` instead of in every
environment's env var.

### 6. 🔲 Dev done, prod pending — Cloud Run deploy pipeline (Stage H)

[#26](https://github.com/ouracademy/srtomy-api/pull/26) (open). Built with Terraform
(`infra/terraform/`) instead of one-off `gcloud` commands, since this needs to be
reproducible across two environments (`sr-tommy-dev`, `sr-tommy-prod`). Covers: WIF
(scoped to `ouracademy/srtomy-api`, branch `main` only), a dedicated deploy service
account, Artifact Registry repo, a Neon Postgres project per environment (replacing the
long-gone ElephantSQL setup), and Secret Manager secrets wired straight from Neon's
generated credentials. `deploy-dev.yml` (auto on `main`) and `deploy-prod.yml` (manual,
gated by a `production` GitHub Environment) build via Cloud Native Buildpacks and deploy
to Cloud Run.

Applied and confirmed live for `sr-tommy-dev`. `sr-tommy-prod`'s Terraform is written but
**not yet applied** — see `infra/terraform/README.md` for the exact steps once that's
ready to go.

### 7. ✅ DONE — Observability: structured logging (Stage I)

[#24](https://github.com/ouracademy/srtomy-api/pull/24). Added
`spring-cloud-gcp-starter-logging` + a `logback-spring.xml` using its bundled
`logback-json-appender.xml` (Stackdriver JSON layout) for every profile except `local`,
which keeps Boot's normal human-readable console output. Done ahead of the deploy
pipeline (item 6) rather than after, since there was no reason to wait.

## Also worth doing (not yet staged as its own PR — flagging so it doesn't get lost)

- **`cors_origin: "*"`** in both `application.yml` and `application-prod.yml` is wide open.
  Now that the API requires auth, a stolen/leaked token from a malicious origin is a more
  realistic risk than it was pre-auth. Scope it to the actual srtomy-ui origin(s) once a
  real domain exists.
- **`hikari.maximumPoolSize: 2`** in `application-prod.yml` — inherited from an old
  ElephantSQL free-tier limit, already flagged with a comment, never revisited against
  whatever Postgres is actually in front of Cloud Run. Worth confirming real limits before
  item 6 (Cloud Run deploy) goes live — a pool too large for the DB tier will just produce
  connection errors under load.
- **`osv-scanner --no-resolve`** in CI was a workaround for scanning a SNAPSHOT parent.
  The parent has been GA (`4.1.1`) since PR #3 — worth trying the flag's removal now to get
  full dependency resolution in the scan instead of the degraded mode.
- ~~**`Complaint.dni`** has no `@NotBlank`/`@NotNull`...~~ ✅ DONE —
  [#21](https://github.com/ouracademy/srtomy-api/pull/21), merged. Resolved the other
  direction from what was originally proposed here: rather than requiring `dni`, migrated
  the DB (`V1.0.9__make_complaint_dni_optional.sql`) to make it genuinely optional, since
  not every complaint has an identity document.
- **Per-role authorization**: still flat "authenticated = full access" (confirmed
  acceptable for now in the original auth design). The data model (`AccountabilityType`,
  roles sync) already supports real roles — this is a deliberate scope cut, not an
  oversight, but worth revisiting once there's more than one kind of user hitting the API.

## Verification

Each item: `./mvnw -q -B verify` green, `python3 scripts/check-language.py` clean, small
commit, own PR — same convention as the rest of this repo (see `AGENTS.md`).
