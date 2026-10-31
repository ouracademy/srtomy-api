# Feature Specification: Editar eventos y cursos

**Feature Branch**: `edit-events`

**Created**: 2026-10-29

**Status**: Draft → Implemented (API); la UI va en el repo `srtomy-ui` (`specs/013-edit-events`)

**Input**: Hoy no se puede editar un evento o curso ya creado: `PUT /v1/events/{id}` existe pero `EditEventCommandHandler` lanza `NotImplementedException("TODO:")` (deuda 21 de `docs/redesign.md`). Se quiere editarlo de punta a punta (API + UI), incluidas las series (`EventChangeOption`).

## Decisiones

Las tres primeras las tomó el dueño del producto; el resto las decide esta spec.

1. **Campos editables**: `name`, `startTime`/`endTime`, `address`, `description`, `speakerIds` y la regla de repetición (`recurrence`, `timeZone`). **No** se pueden cambiar `organizationId` (moverlo cambiaría permisos, compartidos e inscripción) ni `isACourse` («evento» o «curso» queda fijo al crear). El request sigue siendo el `EventRequest` de crear: si `organizationId` o `isACourse` difieren de lo guardado, **400** con mensaje en español; si coinciden, se ignoran.
2. **Cambiar lo que afecta a los inscritos: se permite, se conserva todo y se avisa por correo.** Las inscripciones (`event_registration`), el aforo, la asistencia y los enlaces cuelgan de `event_id`, que no cambia. Se avisa a cada inscrito no cancelado de la ocurrencia que cambió cuando cambia **la fecha u hora, el nombre, el lugar o los ponentes** (el lugar o el ponente pueden afectar a quien va a asistir). Un solo correo por persona (tipo `EVENT_CHANGED`, por el outbox, en la misma transacción) dice qué cambió (con el lugar nuevo y los nombres de los ponentes) y las fechas de sus ocurrencias. Cambiar solo la descripción no envía correos. La UI avisa **antes** de guardar que se enviarán.
3. **Cambiar la regla de repetición o el día de una serie: se conserva lo que ya tiene gente.** Las ocurrencias afectadas que ya pasaron o que tienen inscritos (no cancelados) o asistencia **no se borran ni se modifican**: quedan como eventos sueltos (fuera de la serie). Solo se reemplazan las demás. La respuesta dice cuántas se conservaron y la UI lo avisa antes de guardar («las ocurrencias con inscritos o asistencia no cambian») y después. Como esas ocurrencias no cambian, no se envía correo por ellas.
4. **`EventChangeOption`** (en un evento suelto la opción se ignora):
   - `THIS_EVENT`: se edita solo esa ocurrencia y **sale de la serie** (queda como evento suelto). La `recurrence` del request se ignora. Si cambia la hora, se avisa a sus inscritos.
   - `THIS_AND_THE_FOLLOWING_EVENTS`: la serie **se parte en dos**. La original termina el día anterior (o se borra si no queda ninguno antes) y las ocurrencias desde esta pasan a una serie nueva con los datos editados. Si no se pidió otra regla, la nueva hereda la de la original; si es `COUNT`, lleva solo las repeticiones que faltaban.
   - `ALL_EVENTS`: se edita toda la serie (la misma serie, no se crea otra).
   Para las dos últimas hay dos caminos según qué se cambió:
   - **Sin cambio de regla ni de día** (solo nombre, lugar, ponentes, descripción, hora o duración): se editan **en el lugar** todas las ocurrencias afectadas, conservando su id, inscripciones y asistencia. Una hora nueva se aplica a las ocurrencias futuras (las pasadas conservan la suya) y se avisa a sus inscritos. Las pasadas reciben igual los cambios descriptivos.
   - **Con cambio de regla, de zona horaria o de día** (la fecha de la ocurrencia editada se movió): se **regenera**: las ocurrencias afectadas sin inscritos, sin asistencia y futuras se borran (borrado lógico) y se crea una serie nueva con el horizonte móvil de 12 meses; las que se conservan (decisión 3) quedan como eventos sueltos y no se duplican (no se genera otra ocurrencia en su misma hora). En `ALL_EVENTS` el día de la serie se corre los mismos días que se movió la ocurrencia editada. Nunca se regeneran ocurrencias pasadas.
   En ambos caminos, **la ocurrencia que se editó se aplica exactamente como se envió** (es el ancla de la edición), aunque tenga inscritos; en el camino de regeneración pasa a ser la primera ocurrencia de la serie nueva.
5. **Evento suelto + `recurrence`**: convierte el evento en una serie cuya primera ocurrencia es ese mismo evento (conserva su id e inscripciones); se generan las demás.
6. **`recurrence` ausente** en una serie (`THIS_AND_…`/`ALL_EVENTS`) significa «la regla no cambia». Dejar de repetir se hace borrando «este y los siguientes». `timeZone` ausente: se conserva la de la serie (UTC en un evento suelto).
7. **Permisos**: igual que hoy, `EDITOR` sobre el evento (el nodo, un compartido como Editor o el acceso general). La edición no cambia permisos, compartido ni inscripción.
8. **Eventos compuestos (spec 012, otra rama)**: al cambiar fechas de un mega evento, sus sub-eventos deben seguir dentro. Aquí no se implementa; el handler deja un punto de extensión (`validateSchedule`) y se anota en la deuda.
9. **Fuera de alcance**: editar la inscripción (tiene su propio panel), cambiar el nodo, aviso por WhatsApp, avisar por correo a quien tiene el evento compartido, deshacer una edición.

## User Scenarios & Testing

### US1 — Corregir los datos de un evento (P1)
1. **Given** un evento suelto sin inscritos, **When** un Editor cambia nombre, lugar, ponentes y descripción, **Then** `GET /events/{id}` devuelve los nuevos.
2. **Given** inscritos, **When** cambia solo la descripción, **Then** nadie recibe correo; si cambia el nombre, el lugar o los ponentes, sí.
3. **Given** un evento, **When** se envía otro `organizationId` o `isACourse`, **Then** 400 y no cambia nada.
4. **Given** un usuario sin acceso Editor, **Then** 403; evento inexistente, 404.

### US2 — Mover la fecha de un evento con inscritos (P1)
1. **Given** un evento con dos inscritos y uno cancelado, **When** se cambia la hora, **Then** el evento se mueve, conserva sus inscritos y asistencia, y se encolan 2 correos (no al cancelado).

### US3 — Editar una serie (P1)
1. **Given** una serie semanal, **When** se edita `THIS_EVENT`, **Then** solo esa cambia y sale de la serie.
2. **When** se edita `THIS_AND_THE_FOLLOWING_EVENTS` (otro nombre), **Then** las anteriores conservan el nombre y las siguientes tienen el nuevo, en una serie nueva.
3. **When** se edita `ALL_EVENTS` (otra hora), **Then** todas las futuras cambian de hora conservando id e inscritos.
4. **When** se cambia la regla (de semanal a diaria) con `ALL_EVENTS`, **Then** se regeneran las futuras sin gente y las que tienen inscritos o asistencia se conservan sueltas e intactas; la respuesta dice cuántas.

### US4 — Convertir un evento en repetido (P2)
1. **Given** un evento suelto, **When** se edita con una `recurrence`, **Then** pasa a ser la primera ocurrencia de una serie nueva.

## Requirements

- **FR-001**: `PUT /events/{id}` recibe `EditEventRequest` (`EventRequest` + `option`) y responde `EditEventResponse {id, keptEvents, notifiedRegistrations}`.
- **FR-002**: `EditEventCommand` lleva todos los datos; `EditEventCommandHandler.handle` los aplica en una transacción (todo o nada, incluidos los correos del outbox).
- **FR-003**: validar nombre, fin posterior al inicio, ponentes existentes, repetición válida y que la serie nueva genere al menos una ocurrencia; errores de dominio dan 400.
- **FR-004**: el correo a inscritos sale solo si cambió el inicio, el fin, el nombre, el lugar o los ponentes de su ocurrencia, nunca a inscripciones canceladas, una vez por inscrito.
- **FR-005**: nada de lo editado cambia `share_slug`, `general_access`, la inscripción ni los compartidos.

## Success Criteria
- `./mvnw -B -ntp verify` y `python3 scripts/check-language.py` en verde, con pruebas unitarias, de controlador y de integración (`EditEventsIntegrationTests`).
