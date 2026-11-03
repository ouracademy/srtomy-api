# Implementation Plan: Identidad de usuario e IAM jerárquico

**Branch**: `claude/app-flow-redesign-nq6j60` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)

## Summary

Introduce users, GCP-style access roles inherited down the organization tree, and per-church ministerial positions (cargos).
- A user (`app_user`) is created on first `/me` from the JWT `sub`.
- A church is created atomically with its first location and its positions, and the creator gets the Owner binding.
- Every endpoint asks the new `iam` module whether the JWT subject has a permission on the affected node. The answer comes from the role bindings on that node and on its ancestors, found through `organization.parent_id` with a recursive CTE.
- The Auth0 Management API integration (`auth` module) goes away, together with its config and the resilience4j dependency.

## Technical Context

- **Language/Version**: Java 21, Spring Boot 4.1.1.
- **Primary Dependencies**: Spring Web, Spring Data JPA (Hibernate 7), Spring Security OAuth2 resource server, Flyway. One dependency is removed (`spring-cloud-starter-circuitbreaker-resilience4j`) and none is added.
- **Storage**: PostgreSQL 18.
  - New tables `app_user`, `iam_role` and `role_binding`.
  - New columns on `organization`.
  - Native `text[]` for permissions, mapped by Hibernate as `List<String>`, the same way `event.days_of_week` is mapped.
- **Testing**:
  - JUnit 5 + Mockito for handlers.
  - `@WebMvcTest` + `MockMvcTester` for controllers.
  - `IamIntegrationTests` (`@SpringBootTest`, real Postgres) for inheritance and the 403 paths end to end.
- **Constraints**: ArchUnit module isolation (`ArchitectureTests`); English-only code (`scripts/check-language.py`); Spotless.
- **Scale/Scope**: churches with tens of nodes and a handful of levels. A recursive CTE per authorized request is cheap at that size, so there is no cache for now.

## Constitution Check

| Principle | How this plan complies |
| --- | --- |
| I. Quality gates | `./mvnw verify` stays the gate; the JaCoCo floor is unchanged. |
| II. Test at the right level | Unit tests per handler, `@WebMvcTest` per controller (including a 403 case), and one `*IntegrationTests` for the real SQL. |
| III. Security by default | Adds the missing authorization. Removes the client-supplied `userId` and the client-credentials secret. |
| IV. English code | New modules, permission strings and identifiers are in English. Only error messages are in Spanish. |
| V. Simplicity | `parent_id` + recursive CTE instead of a materialized `path`. Drops resilience4j once it has no use. |
| VI. Small changes | Split into commits per step (migration, user, iam, positions, enforcement, removal of auth). |

## Design decisions

1. **Hierarchy**:
   - `organization.parent_id` (FK, nullable), mapped as `@ManyToOne Organization parent`. `OrganizationWriter.create` validates the parent type: CHURCH has no parent; LOCATION goes under CHURCH; GROUP or MINISTRY goes under LOCATION, GROUP or MINISTRY. A violation throws `shared.domain.DomainValidationException`, which becomes a 400.
   - The IS_PART_OF accountability is still written, because member listings read it.
   - `FindOrganizationChildren` reads from `parent_id`.
   - Ancestor lookups use `WITH RECURSIVE`, so they work at any depth.
2. **Users** (`user` module):
   - Entity `AppUser(id, auth0Sub unique, email, emailVerified)`.
   - `SyncCurrentUserCommand(subject, email, emailVerified)` upserts the user. It is called by `GET /me` and by church creation.
   - Email comes from the standard `email` claim, or from the namespaced `https://sr-tommy.com/email` (Auth0 Actions must namespace custom access-token claims).
   - The link to a person (`person_id`) is phase 005; its column is created now, nullable.
3. **IAM** (`iam` module):
   - `Permission` enum. Each value has a wire value (`org.read`, …). The enum is the catalog.
   - `IamRole(id, name, description, churchId?, permissions List<String>, deletedTime)`. The 5 predefined roles are seeded by the migration with fixed UUIDs, mirrored in `PredefinedRoles`.
   - `RoleBinding(id, user, role, organization, grantedBy, createdDate)`.
   - Queries:
     - `AuthorizeQuery(subject, organizationIds, permission)` → `AuthorizeResult`. It throws Spring's `AccessDeniedException` when none of the nodes grants the permission. The handler-returns-Result rule holds; denial is exceptional. `GlobalExceptionHandler` maps that exception to 403 problem+json.
     - `GetEffectivePermissionsQuery(subject, organizationId)`.
     - `FindAccessibleOrganizationsQuery(subject)`: the nodes with a binding plus their descendants (marked accessible) and their ancestors (breadcrumb only), with the role names bound on each.
   - Command `GrantRoleCommand(userId, roleId, organizationId, grantedBy)`.
4. **Enforcement**: each controller method calls `authorizeQueryHandler.handle(...)` before doing anything, with the node it acts on:

   | Endpoint                                                             | Node                          | Permission          |
   | -------------------------------------------------------------------- | ----------------------------- | ------------------- |
   | `GET organizations/{id}`, `{id}/childs`                              | id                            | `org.read`          |
   | `PUT organizations/{id}`, `PUT locations/{id}`, `PUT locations/childs/{id}` | id                     | `org.update`        |
   | `DELETE organizations/{id}`                                          | id                            | `org.delete`        |
   | `POST locations`, `POST locations/childs`                            | parentId                      | `org.create_child`  |
   | `GET {id}/member-state`, `GET locations/childs/{id}/members`         | id                            | `members.read`      |
   | `GET organizations/{id}/people/{personId}/positions`                 | id                            | `members.read`      |
   | `GET people?organizationId`                                          | organizationId                | `members.read`      |
   | `GET people/{id}`                                                    | the person's organizations    | `members.read`      |
   | `POST people`, `PUT people/{id}`                                     | organizationId (and, on PUT, the person must belong to it) | `members.write` |
   | `GET events?organizationId`                                          | organizationId                | `events.read`       |
   | `GET events/{id}`                                                    | the event's organization      | `events.read`       |
   | `POST events`                                                        | organizationId                | `events.write`      |
   | `PUT/DELETE events/{id}`                                             | the event's organization      | `events.write`      |
   | `GET events/{id}/attendance`                                         | the event's organization      | `events.read`       |
   | `PUT events/{id}/attendance`                                         | the event's organization      | `attendance.write`  |
   | `GET organizations/{id}/positions`                                   | id (the list is the church's) | `org.read`          |
   | `POST organizations/{id}/positions`, `PUT/DELETE positions/{id}`     | the church                    | `positions.manage`  |
   | `POST organizations` (church), `/me*`, `GET positions/suggestions`, `POST complaints` | —            | authenticated only  |

5. **Positions (cargos)**: these are the existing `AccountabilityType` rows with category ROLE, renamed to category `POSITION` and owned by the church.
   - Handlers in `accountability`: `CreatePositions` (several names, case-insensitive dedupe), `RenamePosition`, `DeletePosition` (soft delete), `GetPositionsOfChurch`, `GetPositionsOfPerson`, `SyncPersonPositions` and `GetPositionSuggestions` (a static catalog).
   - `SyncPersonPositions` ignores ids that are not positions of the person's church, so a position from another church cannot be attached.
   - Removed: `GetPredefinedRoles`, `GetRolesOfLocation`, `SyncOrganizationRoles` and the `roles` field of the location requests.
6. **Church creation**: `POST /organizations` takes `CreateChurchRequest`: the church info and social links, a required `location` (the location info and social links), and `positions` (a list of names, which may be empty). In one transaction it:
   1. syncs the user,
   2. creates the church,
   3. creates the location under it,
   4. creates the positions,
   5. grants Owner.
7. **Social links**: an `@Embeddable SocialLinks(website, facebookUrl, instagramUrl, youtubeUrl, tiktokUrl)` inside `OrganizationInfo`. On requests, each link is validated with `@URL(regexp = "^https?://.*")`.
8. **Removal**: delete the `auth` module (gateway, command, config, tests), the `auth0.clientId`/`clientSecret` and `resilience4j` config, and the resilience4j dependency. Terraform still creates the `auth0-client-*` secrets; that clean-up is noted as a follow-up in `docs/redesign.md`, not done here.

## Project Structure

### Documentation (this feature)

```text
specs/003-identity-iam/
├── spec.md
├── plan.md
├── data-model.md
├── contracts/http.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code

```text
src/main/resources/db/migration/V1.1.0__identity_iam.sql
src/main/java/org/ouracademy/ovejafiel/
├── user/        domain (AppUser, UserId), command/sync_current_user, query/find_user_by_subject, repository, controller (MeController)
├── iam/         domain (Permission, IamRole, RoleBinding, PredefinedRoles, ids), command/grant_role,
│                query/{authorize, get_effective_permissions, find_accessible_organizations}, repository
├── organization/  parent, social links, CreateChurchRequest, hierarchy validation, enforcement
├── accountability/ positions handlers + controller/PositionController
├── person/, event/, attendance/  enforcement
└── auth/        removed
```

## Complexity Tracking

None. No constitution deviation.
