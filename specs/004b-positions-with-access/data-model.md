# Data model: 004b-positions-with-access

Migration: `V1.3.0__positions_with_access.sql`.

## position_role (new)

The iam roles whose permissions a POSITION gives. Linked, not copied: editing a role changes every position that uses it. The UI does not expose roles yet; they are in the model so it can later.

| column | type | notes |
| --- | --- | --- |
| position_id | uuid → accountability_type(id) | pk with role_id |
| role_id | uuid → iam_role(id) | never the Owner role (checked by the API) |

## position_permission (new)

Loose permissions of a POSITION. This is what the UI edits: permissions grouped by module. A position created from a suggestion gets a **copy** of the permissions of its base roles.

| column | type | notes |
| --- | --- | --- |
| position_id | uuid → accountability_type(id) | pk with permission |
| permission | varchar(100) | a value of the `Permission` catalog (checked by the API) |

A position with neither roles nor permissions is only a label. What it gives is the union of both.

## iam_role (seeded)

New predefined roles, to build positions from: Ver personas, Gestor de personas, Gestor de eventos, Asistencia, Gestor de estructura (ids in `PredefinedRoles`).

## user_person (new)

Links a signed-in user to the person record that represents them in one church. `app_user.person_id` (reserved, never used) is dropped: one column cannot hold one person per church.

| column | type | notes |
| --- | --- | --- |
| id | uuid pk | |
| user_id | uuid not null → app_user(id) | |
| person_id | uuid not null unique → person(id) | a person has at most one user |
| church_id | uuid not null → organization(id) | |
| unique | (user_id, church_id) | a user has at most one person per church |

## user_access (view)

One row per access a user holds on a node, whatever its source, with the permissions it gives. Every permission query reads it, so the three sources behave the same way (inherited down the tree by the callers). Accesses that give nothing are left out.

| column | notes |
| --- | --- |
| user_id, organization_id | who, on which node |
| source | `DIRECT` (a `role_binding`, in practice Owner), `POSITION`, `LEADER` |
| binding_id | the `role_binding` id, only for `DIRECT` |
| position_name | the position, only for `POSITION` |
| role_id | the role, for `DIRECT` and `LEADER`; null for `POSITION`, which can mix roles and loose permissions |
| permissions | text[] with what the access gives |

Sources:
- `role_binding`: unchanged.
- Positions: `user_person` → `accountability` (person → node, type = a live POSITION) → union of `position_role` and `position_permission`, on the node.
- Leaders: `user_person` → `accountability` of type LEADER → predefined Leader role, on the group.

## Cleanup

`role_binding` rows whose role is not Owner are deleted (no real data; access now comes from positions).
