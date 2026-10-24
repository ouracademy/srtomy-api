# HTTP contract changes: 004b-positions-with-access

All paths are under `/v1` and require a valid JWT. A permission failure returns `403`; a validation error returns `400`.

## Positions

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| GET | `/organizations/{id}/positions` | — | `[{ id, name, accessRoleId, holders }]` (`accessRoleId` null = plain label; `holders` = people who hold it) |
| POST | `/organizations/{id}/positions` | `{ name, accessRoleId? }` | `201 { id, name, accessRoleId }` |
| PUT | `/positions/{id}` | `{ name }` | `{ id, name, accessRoleId }` (rename only) |
| **PUT** | `/positions/{id}/access` | `{ accessRoleId }` (null removes the level) | `{ id, name, accessRoleId }` |
| DELETE | `/positions/{id}` | — | `204` |
| GET | `/positions/suggestions` | — | `[{ name, suggestedRoleId }]` |
| GET | `/organizations/{id}/people/{personId}/positions` | — | `[{ id, name, accessRoleId }]` |

Giving, changing or removing a level, creating or deleting a position that has one, and adding or removing it from a person need `iam.manage` on the node and every permission of the level (`403` otherwise). Owner is rejected as a level (`400`), as is a custom level of another church.

## Access levels

- `GET /iam/levels`: the predefined levels a position can use (Owner excluded), for the sign-up form.
- `GET /organizations/{id}/roles`: adds `assignable` (false for Owner).
- `DELETE /roles/{id}`: `400` while a position uses the level.

## Access list and Owners

- `GET /organizations/{id}/access`: each entry now has `source` (`DIRECT` | `POSITION` | `LEADER`) and `positionName`; `bindingId` is only set for `DIRECT`.
- `POST /organizations/{id}/owners` `{ email }` and `DELETE /organizations/{id}/owners/{bindingId}`: only an Owner; the church keeps at least one.
- **Removed**: `POST /organizations/{id}/access` and `DELETE /organizations/{id}/access/{bindingId}`.

## Church creation

`POST /organizations` adds:

```json
{
  "positions": [{ "name": "Pastor", "accessRoleId": "…" }, { "name": "Hermano servidor" }],
  "creator": { "firstName": "Ana", "lastName": "Diaz", "positionName": "Pastor" }
}
```

`positions` is now a list of objects (it was a list of names). `creator` is required.

## People

`POST /people` and `PUT /people/{id}` keep their shape; they now fail with `403` when `positionIds` add or remove a position that gives more access than the caller holds.
