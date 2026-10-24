# Tasks: Identidad de usuario e IAM jerárquico

**Input**: [plan.md](./plan.md), [data-model.md](./data-model.md), [contracts/http.md](./contracts/http.md)

Every task ships with its tests (constitution II). `[P]` means the task can run in parallel with the others.

## Phase 1: Setup

- [ ] T001 Write the migration `src/main/resources/db/migration/V1.1.0__identity_iam.sql`. It cleans the data, alters `organization` and `accountability_type`, creates `app_user`, `iam_role` and `role_binding`, and seeds the predefined roles.
- [ ] T002 Add `shared/domain/DomainValidationException.java`. In `GlobalExceptionHandler`, map it to 400 and `AccessDeniedException` to 403, with tests in `GlobalExceptionHandlerTests`.
- [ ] T003 Add `"user"` and `"iam"` to `ArchitectureTests.MODULES` (and later remove `"auth"`).

## Phase 2: Foundational (blocks every story)

- [ ] T004 [P] `user` module: the `AppUser`/`UserId` entity, `UserRepository`, `command/sync_current_user` and `query/find_user_by_subject`, with handler tests.
- [ ] T005 [P] `iam` module domain: `Permission`, `IamRole`/`IamRoleId`, `RoleBinding`/`RoleBindingId`, `PredefinedRoles`; repositories with the recursive-CTE native queries.
- [ ] T006 `iam` handlers: `query/authorize`, `query/get_effective_permissions`, `query/find_accessible_organizations` and `command/grant_role`, with unit tests.
- [ ] T007 Organization hierarchy: the `parent` column and `SocialLinks` embeddable in `Organization`, parent-type validation in `OrganizationWriter.create`, `FindOrganizationResult`/`OrganizationResponse` with `parentId` and `socialLinks`, and `FindOrganizationChildren` reading by parent. Update the tests.

## Phase 3: US1 (create my church with its first location, P1)

- [ ] T008 Rewrite `CreateChurchCommand`/handler: user sync, church, location, positions and Owner, all atomic. Remove the Auth0 metadata step. Add `CreateChurchRequest` with `location` and `positions`.
- [ ] T009 Delete the `auth` module (main and test), its config in both `application.yml`, and the resilience4j dependency in `pom.xml`.

## Phase 4: US2 (access by role and inheritance, P1)

- [ ] T010 Enforce `OrganizationController` per the table in plan.md, with a 403 test.
- [ ] T011 [P] Enforce `PersonController`. Add the accountability query `find_organizations_of_member` to resolve a person's nodes.
- [ ] T012 [P] Enforce `EventController` and `AttendanceController`, resolving the node from the event.
- [ ] T013 Add `IamIntegrationTests`: two users and two churches, a role at the church that reaches a node 5 levels deep, and a group role that does not reach its location.

## Phase 5: US3 (who am I and where can I go, P1)

- [ ] T014 `MeController`: `GET /me`, `/me/organizations`, `/me/permissions`, with WebMvc tests.

## Phase 6: US4 (recursive groups, P2)

- [ ] T015 Accept a location, group or ministry as the parent in `CreateLocationChild`; allow empty leaders and members; add a hierarchy test in the integration test.

## Phase 7: US5 (flexible positions, P2)

- [ ] T016 Positions in `accountability`: rename category ROLE to POSITION; add `create_positions`, `rename_position`, `delete_position`, `get_positions_of_church`, `get_positions_of_person`, `sync_person_positions` (filtered to the church) and `get_position_suggestions`. Remove `get_predefined_roles`, `get_roles_of_location`, `get_roles_of_person`, `sync_person_roles` and `sync_organization_roles`.
- [ ] T017 Add `accountability/controller/PositionController`. Remove `roles` from the location requests; rename the person requests' `roles` to `positionIds`.

## Phase 8: Polish

- [ ] T018 Update `docs/code-style.md` (module list), `README.md` (no Auth0 client secret; the `email` claim Action) and `docs/redesign.md` (parent_id + CTE instead of path).
- [ ] T019 Run `./mvnw verify` and `python3 scripts/check-language.py`.
