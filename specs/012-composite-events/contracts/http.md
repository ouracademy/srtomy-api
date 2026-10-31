# Contratos HTTP: Eventos compuestos

| Método y ruta | Quién / permiso | Descripción |
| --- | --- | --- |
| `POST /v1/events` | `events.write` en la organización | Acepta `parentEventId` y `registrationMode` (`INHERIT` por defecto). |
| `GET /v1/events/{id}` | VIEWER | Agrega `parentEventId`, `parentTitle`, `registrationMode` y `children[]` (`id`, `title`, `start`, `end`, `isACourse`, `registrationMode`). |
| `GET /v1/events?organizationId&start&end` | `events.read` | Sin anidar: cada evento trae `parentEventId`, `parentTitle` y `registrationMode`. |
| `PUT /v1/events/{id}` | EDITOR | 400 si un sub-evento queda fuera del mega evento o el mega evento deja a un sub-evento fuera, si cambia de mega evento o si pide `recurrence`. `registrationMode` cambia el modo de un sub-evento. Al mover un sub-evento `INHERIT`, el correo va a los inscritos del mega evento. |
| `DELETE /v1/events/{id}` | MANAGER | Borrar un contenedor borra sus hijos. |
| `GET /v1/public/events/{slug}` | público | Agrega `program[]` con los hijos que el visitante puede abrir. Sin inscritos ni códigos. |
| `POST /v1/public/events/{slug}/registrations` | público | 400 si el evento es un hijo `INHERIT` u `OPEN`. |
| `POST /v1/events/{id}/attendance/check-in` | `attendance.write` | En un hijo `INHERIT` acepta el ticket del padre. |

## Errores

- 400: padre inexistente como hijo, ciclo, fechas fuera del padre, `recurrence` con padre, hijo de un hijo, ticket sin validez en un hijo `OWN`.
- 404: `parentEventId` que no existe.
