# HTTP contract changes: 003-identity-iam

All paths are under `/v1` and require a valid JWT. A permission failure returns `403` as `application/problem+json`. A hierarchy or validation error returns `400`.

## New

### `GET /me`
Upserts the current user from the JWT.
Response: `{ id, email, emailVerified }`.

### `GET /me/organizations`
Every node the user can see, flat. The client builds the tree from `parentId`.

```json
[
  { "id": "…", "name": "Iglesia X", "type": "CHURCH", "parentId": null, "accessible": true, "roles": ["Propietario"] },
  { "id": "…", "name": "Sede Centro", "type": "LOCATION", "parentId": "…", "accessible": true, "roles": [] }
]
```

- `accessible = false` marks ancestors shown only as a breadcrumb.
- `roles` lists the roles bound directly on that node.

### `GET /me/permissions?organizationId={id}`
Response: `["org.read", "members.read", …]`. These are the permissions on the node, inherited from its ancestors included.

### Positions (cargos)

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| GET | `/organizations/{id}/positions` | — | `[{ id, name }]` for the church of `{id}` |
| POST | `/organizations/{id}/positions` | `{ name }` | `201 { id, name }`; `400` if it is a duplicate |
| PUT | `/positions/{id}` | `{ name }` | `{ id, name }` |
| DELETE | `/positions/{id}` | — | `204` |
| GET | `/positions/suggestions` | — | `["Pastor", "Copastor", …]` |
| GET | `/organizations/{id}/people/{personId}/positions` | — | `[{ id, name }]` |

## Changed

### `POST /organizations`: create a church

```json
{
  "name": "Iglesia X", "description": null, "address": null, "phoneNumber": null,
  "socialLinks": { "website": null, "facebookUrl": "https://facebook.com/x", "instagramUrl": null, "youtubeUrl": null, "tiktokUrl": null },
  "location": { "name": "Sede Centro", "description": null, "address": "…", "phoneNumber": null, "socialLinks": null },
  "positions": ["Pastor", "Líder de alabanza", "Servidor", "Maestro"]
}
```

Response `201`: `OrganizationResponse` of the church. `userId` and `parentId` were removed from the request.

### `OrganizationResponse`
Adds `parentId` and `socialLinks`.

### `POST /organizations/locations`, `PUT /organizations/locations/{id}`
The `roles` field is removed. `socialLinks` is accepted. `parentId` must be a church.

### `POST /organizations/locations/childs`, `PUT /organizations/locations/childs/{id}`
- `parentId` may be a location, a group or a ministry.
- `socialLinks` is accepted.
- `leaderIds` and `memberIds` may be empty.

### `POST /people`, `PUT /people/{id}`
`roles` is renamed to `positionIds`: the ids of positions of the church that owns `organizationId`. Ids that are not positions of that church are ignored.

## Removed

- `GET /organizations/locations/predefined-roles`, replaced by `GET /positions/suggestions`.
- `GET /organizations/locations/{id}/roles`, replaced by `GET /organizations/{id}/positions`.
- `GET /organizations/locations/{id}/person/{personId}/roles`, replaced by `GET /organizations/{id}/people/{personId}/positions`.
