# HTTP contract: Inscripción

Administrar (`MANAGER`: `events.write` en el nodo del evento):

| Método y ruta | Descripción |
| --- | --- |
| `GET /events/{id}/registration` | `{enabled, open, requiresAccount, waitlistEnabled, capacity, confirmed, waitlisted, tickets[{id, name, priceCents, currency, quantity, salesStart, salesEnd, sold}]}`; `enabled=false` si no hay inscripción. |
| `PUT /events/{id}/registration` | Body `{open, requiresAccount, waitlistEnabled, capacity?, tickets[{id?, name, priceCents, currency, quantity?, salesStart?, salesEnd?}]}`. Un ticket con `id` se actualiza, sin `id` se crea, y uno que ya no viene se borra (400 si tiene inscripciones). Devuelve lo mismo que el GET. |
| `DELETE /events/{id}/registration` | 204. Quita la inscripción; 400 si hay inscripciones activas. |
| `GET /events/{id}/registrations` | `[{id, firstName, lastName, email, phoneNumber, hasAccount, ticketName, status, registeredAt}]`. |
| `DELETE /events/{id}/registrations/{registrationId}` | 204. La pasa a `CANCELLED` y llena el lugar desde la espera. |

Compartir (`PUT /events/{id}/sharing`): body y respuesta `{generalAccess}` (sin `linkRole`).

Público y Mi espacio:

| Método y ruta | Quién | Descripción |
| --- | --- | --- |
| `GET /public/events/{slug}` | quien pueda abrirlo | Agrega `registration {open, requiresAccount, waitlistEnabled, soldOut, tickets[{id, name, priceCents, currency, available, remaining}]}` (nulo sin inscripción), `canRegister` (puede inscribirse ahora) y `registered`; quita nada más. |
| `POST /public/events/{slug}/registrations` | sin sesión (o con ella) | Body `{ticketTypeId, firstName, lastName, email, phoneNumber?}`; 201 `{alreadyRegistered, status, cancelCode}`; 403 si exige cuenta y no hay sesión, o no puede abrir el evento; 400 si está cerrada, fuera de ventana, agotada o falta algo. |
| `DELETE /public/registrations/{cancelCode}` | sin sesión | 204; 404 si el código no existe. |
| `GET /me/events` | sesión | Agrega por evento `registrationId`, `ticketName`, `registrationStatus`, `cancelCode`; `registered` es verdadero solo con inscripción activa. |

Asistencia por QR (`ATTENDANCE_WRITE` salvo donde se indica):

| Método y ruta | Quién | Descripción |
| --- | --- | --- |
| `GET /events/{id}/attendance/qr` | `ATTENDANCE_WRITE` | `{token, url, expiresAt}`; el `url` es `/a/{token}` en la UI. |
| `GET /public/attendance/{token}` | cualquiera | `{title, start, canMarkSelf}` para mostrar la pantalla de bienvenida; 404 si el token no es válido. |
| `POST /public/attendance/{token}` | con o sin sesión | Body `{name?}`; con sesión marca a la persona vinculada (y su inscripción). Sin sesión, con nombre crea una persona `VISITOR`; sin nombre suma al contador. 200 `{kind: PERSON \| VISITOR \| ANONYMOUS, alreadyMarked}`; 400 si venció o el evento no está en curso. |
| `POST /events/{id}/attendance/check-in` | `ATTENDANCE_WRITE` | Body `{ticketCode}`; 200 `{registrationId, name, ticketName, alreadyCheckedIn, checkedInAt}`; 400 si no está confirmada. |
| `PUT /events/{id}/registrations/{registrationId}/check-in` | `ATTENDANCE_WRITE` | Body `{checkedIn}`: marca o desmarca a mano; devuelve lo mismo que el ingreso por ticket. |
| `POST /events/{id}/attendance/visitors` | `ATTENDANCE_WRITE` | Body `{name, phone?, email?}`: crea una persona `VISITOR` en el nodo del evento y la marca presente. |
| `PUT /events/{id}/attendance/guests` | `ATTENDANCE_WRITE` | Body `{guestCount}`: visitantes sin nombre de ese evento. |

| `GET /me/events` | sesión | Agrega por evento `ticketCode` (para dibujar su QR) y `checkedIn`; `attended` une asistencia de persona e ingreso. |

Estado de la persona: `POST /people` y `PUT /people/{id}` aceptan `memberState` (`VISITOR`, `MEMBERSHIP_IN_PROGRESS`, `MEMBER`; opcional, por defecto visitante al crear y sin cambios al editar). Con cargos el estado es siempre `MEMBER`. Se lee en `GET /organizations/{id}/member-state`.

`GET /events/{id}/attendance` agrega `guestCount` y `marks[{personId, source, markedAt}]`. Los booleanos de los cuerpos (`open`, `requiresAccount`, `waitlistEnabled`) deben enviarse siempre.
