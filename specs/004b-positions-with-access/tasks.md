# Tasks: Cargos con nivel de acceso

**Input**: [plan.md](./plan.md), [data-model.md](./data-model.md), [contracts/http.md](./contracts/http.md)

Every task ships with its tests (constitution II).

## Phase 1: Data

- [x] T001 Migration `V1.3.0__positions_with_access.sql`: `accountability_type.iam_role_id`, `user_person`, drop `app_user.person_id`, delete non-Owner bindings, view `user_access`.
- [x] T002 `user` module: `UserPersonLink` entity, repository and `link_user_to_person` command.

## Phase 2: Permissions from positions (US1, US6)

- [x] T003 `RoleBindingRepository` reads `user_access` (permissions, visible nodes, bound roles, access list).
- [x] T004 `AccountabilityType.accessRoleId`; `PositionResult` carries it.

## Phase 3: Positions with access (US3, US4)

- [x] T005 `AuthorizeAccessGrant` query (the one escalation rule).
- [x] T006 `create_position`, `delete_position`, `sync_person_positions` take the subject and authorize positions with access; new `set_position_access`.
- [x] T007 `get_positions_of_church` returns holders; suggestions carry a default level.
- [x] T008 `delete_role` refuses a level in use; `GET /iam/levels`; `assignable` on roles.

## Phase 4: Church creation (US2)

- [x] T009 `CreateChurch`: Owner first, positions with level, creator person linked to the user.
- [x] T010 Thread the subject through `CreatePerson` / `UpdatePerson` and `PersonController`.

## Phase 5: Who has access (US5)

- [x] T011 `add_owner` / `remove_owner` replace grant/revoke access; access list entries have `source` and `positionName`.

## Phase 6: Polish

- [x] T012 Integration tests: positions give access on the node and below, no link or no level gives none, removing the position or changing the level is immediate, escalation attempts get 403, Owner rules, custom level lifecycle.
- [x] T013 Update `docs/redesign.md` and `README.md`.
- [x] T014 `./mvnw verify` and `python3 scripts/check-language.py`.
