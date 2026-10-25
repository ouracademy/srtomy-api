# HTTP contract: Invitaciones

| Método y ruta | Permiso | Descripción |
| --- | --- | --- |
| `POST /organizations/{id}/invitations` | `invitations.send` en el nodo (+ permisos del cargo) | Body `{email, positionId?, personId?, firstName?, lastName?}` (los nombres son opcionales). Sin `personId`, si una sola persona del nodo tiene ese correo se vincula a ella (varias → 400); con `personId` y sin correo en la ficha, se le guarda el correo invitado (nunca se pisa uno existente). 201 `{invitation, emailSent}` (la invitación incluye `needsName`). Invitar otra vez al mismo correo y nodo reemplaza la anterior. |
| `GET /organizations/{id}/invitations` | `invitations.send` | Pendientes del nodo (las vencidas con `status: EXPIRED`). |
| `DELETE /invitations/{id}` | `invitations.send` en su nodo | Cancela. 204. |
| `GET /public/invitations/{token}` | ninguno | `{email, organizationName, positionName, status, needsName}`. 404 si no existe. |
| `POST /invitations/{token}/accept` | sesión; correo verificado = invitado | Body opcional `{firstName, lastName}`, obligatorio si `needsName` (400 si faltan). `{organizationId, churchId, personId}`. 403 otro correo, 400 usada/vencida/revocada. |
| `GET /me/invitations` | sesión | Pendientes para el correo verificado del usuario. |
| `POST /me/invitations/{id}/accept` | sesión | Igual que aceptar por token. |

`people` acepta y devuelve `email`.
