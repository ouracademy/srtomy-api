# HTTP contract changes: 004b-positions-with-access

All paths are under `/v1` and require a valid JWT. A permission failure returns `403`; a validation error returns `400`.

A position gives the union of the permissions of its `roleIds` and its loose `permissions`. The UI only uses `permissions`; `roleIds` are there for a future role-aware UI.

## Positions

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| GET | `/organizations/{id}/positions` | — | `[{ id, name, roleIds, permissions, effectivePermissions, holders: [{ personId, name, organizationName, email }] }]` (`email` null = the person has no account) |
| POST | `/organizations/{id}/positions` | `{ name, roleIds?, permissions? }` | `201 { id, name, roleIds, permissions }` |
| PUT | `/positions/{id}` | `{ name }` | `{ id, name, roleIds, permissions }` (rename only) |
| **PUT** | `/positions/{id}/access` | `{ roleIds?, permissions? }` (both empty = plain label) | `{ id, name, roleIds, permissions }` |
| DELETE | `/positions/{id}` | — | `204` |
| GET | `/positions/suggestions` | — | `[{ name, permissions }]`: the permissions are a copy of what the base roles give |
| GET | `/organizations/{id}/people/{personId}/positions` | — | `[{ id, name, roleIds, permissions }]` |

Giving, changing or removing permissions, creating or deleting a position that gives some, and adding or removing it from a person need `iam.manage` on the node **and every effective permission** of the position (`403` otherwise). Owner is rejected as a role (`400`), as is a custom role of another church or an unknown permission.

## Roles

- `GET /iam/levels`: the predefined roles a position can use (Owner excluded).
- `GET /organizations/{id}/roles`: adds `assignable` (false for Owner).
- `DELETE /roles/{id}`: `400` while a position uses the role.

## Access list and Owners

- `GET /organizations/{id}/access`: each entry has `source` (`DIRECT` | `POSITION` | `LEADER`), `positionName`, and `permissionCount`; `roleId` and `roleName` are null for `POSITION`; `bindingId` is only set for `DIRECT`.
- `POST /organizations/{id}/owners` `{ email }` and `DELETE /organizations/{id}/owners/{bindingId}`: only an Owner; the church keeps at least one.
- **Removed**: `POST /organizations/{id}/access` and `DELETE /organizations/{id}/access/{bindingId}`.

## Church creation

`POST /organizations` adds:

```json
{
  "positions": [{ "name": "Pastor", "permissions": ["org.read", "members.write"] }, { "name": "Hermano servidor" }],
  "creator": { "firstName": "Ana", "lastName": "Diaz", "positionName": "Pastor" }
}
```

`positions` is now a list of objects (it was a list of names; each takes `roleIds` and/or `permissions`). `creator` is required.

## People

`POST /people` and `PUT /people/{id}` keep their shape; they now fail with `403` when `positionIds` add or remove a position that gives more than the caller holds.
