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
