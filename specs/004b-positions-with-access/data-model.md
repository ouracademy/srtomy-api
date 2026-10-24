# Data model: 004b-positions-with-access

Migration: `V1.3.0__positions_with_access.sql`.

## accountability_type (altered)

| column | type | notes |
| --- | --- | --- |
| iam_role_id | uuid null → iam_role(id) | access level of a POSITION; null = label only. Never the Owner role (checked by the API). |

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

One row per access a user holds on a node, whatever its source. Every permission query reads it, so the three sources behave the same way (inherited down the tree by the callers).

| column | notes |
| --- | --- |
| user_id, role_id, organization_id | who, which level, on which node |
| source | `DIRECT` (a `role_binding`, in practice Owner), `POSITION`, `LEADER` |
| binding_id | the `role_binding` id, only for `DIRECT` |
| position_name | the position, only for `POSITION` |

Sources:
- `role_binding`: unchanged.
- Positions: `user_person` → `accountability` (person → node, type = a live POSITION with `iam_role_id`) → role of the position, on the node.
- Leaders: `user_person` → `accountability` of type LEADER → predefined Leader role, on the group.

## Cleanup

`role_binding` rows whose role is not Owner are deleted (no real data; access now comes from positions).
