# HTTP contract: Invitaciones

| Método y ruta | Permiso | Descripción |
| --- | --- | --- |
| `POST /organizations/{id}/invitations` | `invitations.send` en el nodo (+ permisos del cargo) | Body `{email, positionId?, personId?, firstName?, lastName?}`. 201 `{invitation, emailSent}`. Invitar otra vez al mismo correo y nodo reemplaza la anterior. |
| `GET /organizations/{id}/invitations` | `invitations.send` | Pendientes del nodo (las vencidas con `status: EXPIRED`). |
| `DELETE /invitations/{id}` | `invitations.send` en su nodo | Cancela. 204. |
| `GET /public/invitations/{token}` | ninguno | `{email, organizationName, positionName, status}`. 404 si no existe. |
| `POST /invitations/{token}/accept` | sesión; correo verificado = invitado | `{organizationId, churchId, personId}`. 403 otro correo, 400 usada/vencida/revocada. |
| `GET /me/invitations` | sesión | Pendientes para el correo verificado del usuario. |
| `POST /me/invitations/{id}/accept` | sesión | Igual que aceptar por token. |

`people` acepta y devuelve `email`.
