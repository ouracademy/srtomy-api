# Implementation Plan: Cargos con nivel de acceso

**Branch**: `claude/positions-with-access` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)

## Summary

The position (cargo) gives permissions: those of its roles (`position_role`) plus loose ones (`position_permission`). A user gets them on the node where the person linked to them holds the position, and below it. The permission engine is untouched: its queries now read the `user_access` view, which merges direct bindings (Owner), position-derived access and group leadership. Loose role granting by email is removed; Owner has its own endpoints.

## Constitution Check

| Principle | How |
| --- | --- |
| II. Tests | Handler unit tests for every changed or new command and query, `@WebMvcTest` controller tests with 403 cases, and integration tests over the real schema (view, links, escalation). |
| III. Security | The anti-escalation rule lives in one query (`AuthorizeAccessGrant`) used by every handler that gives, changes or takes a position with access. Nobody gets access just because someone types their email. |
| IV. English code | Identifiers in English; only user-facing messages in Spanish. |
| V. Simplicity | One view instead of three code paths; no cache; no new permission. |

## Design decisions

1. **One source for permissions**: view `user_access(user_id, role_id, organization_id, source, binding_id, position_name)`. `RoleBindingRepository` reads it for permissions, visible nodes, bound roles and the access list; the recursive CTE over `parent_id` is unchanged.
2. **Who gets the access**: users are linked to persons by `user_person` (one person per user per church, one user per person). Only the church creator is linked in this phase; invitations (005) link the rest.
3. **Owner stays direct**: `role_binding` keeps only Owner. The migration deletes the other bindings (no real data). `POST/DELETE organizations/{id}/owners` replace the old access endpoints; only an Owner adds or removes Owners and the church keeps at least one.
4. **Escalation rule**: `AuthorizeAccessGrantQuery(subject, roleIds, permissions, nodeId)` needs `iam.manage` on the node, rejects Owner, roles of another church and unknown permissions, expands the union (`ExpandPermissionsQuery`) and applies `GrantRules.requireHoldsAll`: the caller must hold every effective permission. It runs when creating, deleting, or changing the level of a position that has access, and when adding or removing such a position from a person. Positions without a level only need the permission they needed before.
5. **Custom roles** (004) stay in the API and the model, but the UI does not show roles for now. A role used by a position cannot be deleted (400 with the count).
   **Suggestions** are templates (name + base roles) expanded to a copy of their permissions; finer predefined roles (Ver personas, Gestor de personas, Gestor de eventos, Asistencia, Gestor de estructura) are seeded as bricks.
6. **Church creation** (`POST /organizations`): Owner is granted first, then positions are created with their permissions (checked like any other grant), then the creator's person is created in the first location holding the creator's position (created as a label if not listed) and linked to the user.
7. **Group leaders**: the `LEADER` accountability gives the Leader level on that group to a linked user (third branch of the view).
8. **Out of scope**: the "Miembro" level for any person with an account and no position (phase 008), and linking people other than the creator (phase 005).

## Endpoints

See [contracts/http.md](./contracts/http.md).

## Source Code

```text
src/main/resources/db/migration/V1.3.0__positions_with_access.sql
user/{domain/UserPersonLink, repository/UserPersonLinkRepository, command/link_user_to_person}
iam/{command/add_owner, command/remove_owner, query/authorize_access_grant, query/list_predefined_roles}
accountability/{command/set_position_access, query/PositionSummaryResult} (+ changed create/delete/sync positions)
organization/command/create_church (creator + levels)
```
