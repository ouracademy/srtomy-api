# Tasks: Gestión de accesos y roles personalizados

Every task ships with its tests. See [plan.md](./plan.md).

- [ ] T001 Migration `V1.2.0__custom_roles.sql`.
- [ ] T002 `user` queries `find_user_by_email` and `find_user_by_subject`.
- [ ] T003 `IamRole` factory/update/soft delete; repositories (access list, holds role, owner count, role lookups).
- [ ] T004 `GrantRules` and `RoleRules`, with tests.
- [ ] T005 [US1] `list_access`, `list_roles`, `find_role_by_id`, `get_permission_catalog`.
- [ ] T006 [US2] `grant_access` and `revoke_access` commands.
- [ ] T007 [US3] `create_role`, `update_role`, `delete_role` commands.
- [ ] T008 `AccessController` with `@WebMvcTest` covering 403.
- [ ] T009 `IamAccessIntegrationTests`: inheritance listing, anti-escalation, Admin vs Owner, last Owner, custom role lifecycle.
- [ ] T010 Docs: README/code-style/redesign (§9 numbering, §10 debt) and `./mvnw verify` + `scripts/check-language.py`.
