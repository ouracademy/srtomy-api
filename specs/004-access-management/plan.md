# Implementation Plan: Gestión de accesos y roles personalizados

**Branch**: `claude/004-access-management` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)

## Summary

Adds, on top of the 003 `iam` module, the endpoints and rules to list who has access to a node (direct and inherited), grant and revoke role bindings, and manage per-church custom roles. No new tables: `iam_role.church_id`, `deleted_time` and `role_binding.granted_by` already exist from `V1.1.0`.

## Technical Context

Same stack as 003 (Java 21, Spring Boot 4, JPA, Postgres, ArchUnit, Spotless). No new dependency, no migration (a partial unique index on `(church_id, lower(name))` for live custom roles is added in `V1.2.0__custom_roles.sql`, see data-model).

## Constitution Check

| Principle | How |
| --- | --- |
| II. Tests | Handler unit tests, `@WebMvcTest` controller tests with 403 cases, and integration tests over the real SQL. |
| III. Security | Anti-escalation rules live in the handlers, not the controller; endpoints authorize first. |
| IV. English code | Identifiers in English; only user-facing error messages in Spanish. |
| V. Simplicity | Reuses `role_binding` and `iam_role`; no cache; the "based on a predefined role" shortcut is UI-only. |

## Design decisions

1. **Where the rules live**: two `public` helpers in `iam/command/`: `GrantRules` (who may grant which role on a node) and `RoleRules` (name and permission validation for custom roles). Handlers take the caller's JWT `subject`; the controller still calls `AuthorizeQueryHandler` first for `iam.read` / `iam.manage`.
2. **Grant rule (FR-005)**: allowed if the caller holds the role on the node or an ancestor, or the caller's effective permissions on the node include all the role's permissions. Owner is the exception: only an Owner grants or revokes it. A custom role is only grantable inside its own church.
3. **Last Owner (FR-006)**: revoking an Owner binding counts the distinct users with an Owner binding on the church's tree; if the target is the only one, `DomainValidationException` (400).
4. **Target user**: found by verified email through the `user` module (`FindUserByEmailQuery`); the caller through `FindUserBySubjectQuery`. Unknown email is a 400 (invitations are phase 005).
5. **Access list**: a recursive CTE collects the node and its ancestors and joins their bindings, users, live roles and node names. `inherited` is `bindingOrganization != requested node`.
6. **Custom roles**: `IamRole` gets a factory, `update` and `softDelete`. Predefined roles (no church) throw `DomainValidationException` on edit or delete. Permissions are checked against the `Permission` catalog; the editor must hold every permission they assign (Owner holds all). Name uniqueness (case-insensitive) is checked against predefined roles and live roles of the church. Deleted roles are already hidden from effective permissions by `@SQLRestriction` and the CTE filters.
7. **Catalog**: `GET /iam/permissions` returns the permissions grouped by resource (prefix before the dot).
8. **Out of scope**: automatic Líder binding on `LEADER` accountability (decided with the product owner: phase 005).

## Endpoints

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET | `/organizations/{id}/access` | `iam.read` on id | direct + inherited |
| POST | `/organizations/{id}/access` | `iam.manage` on id | `{ email, roleId }` |
| DELETE | `/organizations/{id}/access/{bindingId}` | `iam.manage` on id | direct only |
| GET | `/organizations/{id}/roles` | `iam.read` on id | predefined + church roles |
| POST | `/organizations/{id}/roles` | `iam.manage` on the church | `{ name, description, permissions }` |
| PUT | `/roles/{roleId}` | `iam.manage` on its church | |
| DELETE | `/roles/{roleId}` | `iam.manage` on its church | soft delete |
| GET | `/iam/permissions` | authenticated | catalog by resource |

## Source Code

```text
iam/command/{GrantRules,RoleRules}, grant_access, revoke_access, create_role, update_role, delete_role
iam/query/{RoleResult}, list_access, list_roles, find_role_by_id, get_permission_catalog
iam/controller/AccessController (+dto)
user/query/{find_user_by_email, find_user_by_subject}
```
