# HTTP contract: Compartir

Administrar (`MANAGER`: `events.write` en el nodo del evento):

| Método y ruta | Descripción |
| --- | --- |
| `GET /events/{id}/sharing` | `{generalAccess, linkRole, slug, isACourse, shares[{id,email,role}], registrations}`. |
| `PUT /events/{id}/sharing` | Body `{generalAccess, linkRole}`; 400 si `REGISTRANT` y no es curso. Devuelve lo mismo que el GET. |
| `POST /events/{id}/sharing/link/renew` | `{slug}` nuevo; el anterior deja de servir. |
| `POST /events/{id}/shares` | Body `{email, role}`; 201 `{share, emailSent}`. De nuevo con el mismo correo: cambia el rol. |
| `DELETE /events/{id}/shares/{shareId}` | 204. |
| `GET /events/{id}/registrations` | `[{id, firstName, lastName, email, phoneNumber, hasAccount, registeredAt}]`. |
| `DELETE /events/{id}/registrations/{registrationId}` | 204. |

Compartidos conmigo y página del enlace:

| Método y ruta | Quién | Descripción |
| --- | --- | --- |
| `GET /me/shared-events` | sesión | `[{title, start, isACourse, organizationName, role, slug}]`. |
| `GET /public/events/{slug}` | quien pueda abrirlo (con o sin token) | `{title, description, address, start, end, isACourse, organizationName, daysOfWeek, endRecur, canRegister, registered}`; 404 si no. |
| `POST /public/events/{slug}/registrations` | sin sesión (o con ella) | Body `{firstName, lastName, email, phoneNumber?}`; 201 `{alreadyRegistered}`; 403 si el enlace no permite inscribirse a ese visitante; 400 si falta algo. |

`GET /events/{id}` ahora acepta a quien lo ve por un compartido o por `CHURCH`; `PUT` pide `EDITOR`; `DELETE`, `MANAGER`.
