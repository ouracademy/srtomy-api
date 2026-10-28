# Feature Specification: Eventos compuestos (mega evento con sub-eventos y cursos)

**Feature Branch**: `ccr-5c538956-covxf8`

**Created**: 2026-10-28

**Status**: Implemented (API). La UI vive en el repo `srtomy-ui` y falta.

**Input**: Una iglesia tiene cada año un mega evento de 3 días con eventos cortos y cursos dentro. Hoy no hay jerarquía entre eventos: un curso es un `Event` con `is_a_course`, y lo único que agrupa eventos es la serie recurrente (`event_series`). `Event.java` ya anticipa los «eventos complejos» en un comentario, pero nunca se implementó.

## Decisiones (acordadas con el dueño del producto)

1. **Un solo nivel: contenedor → hijos.** El mega evento es un `Event` que contiene sub-eventos y cursos. Un hijo no puede tener hijos. Dos niveles (mega evento → curso → sesiones) quedan como deuda técnica.
2. **Sin tabla nueva.** Se agrega `event.parent_event_id` (nulo = evento suelto o contenedor). Un evento es *contenedor* si tiene hijos. `is_a_course` sigue siendo una bandera de presentación y los cursos son hijos con `is_a_course = true`.
3. **La inscripción se hace al mega evento y los hijos la heredan**, como una herencia en orientación a objetos: cada hijo puede sobrescribirla. `event.registration_mode`, solo para hijos:
   - `INHERIT` (por defecto): vale el ticket `CONFIRMED` de la inscripción del padre.
   - `OWN`: el hijo usa su propia configuración y tickets (`event_registration_setup`, `ticket_type`), p. ej. un sub-evento especial con cupo aparte.
   - `OPEN`: el hijo es libre, no pide inscripción.
   En un evento sin padre el campo se ignora (equivale a `OWN`).
4. **Cupos y tickets viven donde se configuran.** Los del mega evento están en el padre; un hijo `OWN` tiene los suyos. `Seats` no cambia.
5. **La asistencia sigue siendo por evento** (`event_attendance` es 1:1 con `event`): cada sub-evento tiene su lista. El QR de un hijo marca en ese hijo; el QR del padre es la entrada general del mega evento.
6. **Check-in por ticket heredado.** Escanear el `ticket_code` de la inscripción del padre en un hijo `INHERIT` deja ingresada la asistencia de **ese hijo**; en un hijo `OWN` sin ticket propio responde 400. Si la inscripción no tiene una persona de la iglesia vinculada también responde 400, porque la asistencia de un hijo es por persona (deuda técnica 32).
7. **Reglas de forma.** Sin ciclos; el padre no puede ser hijo; el hijo pertenece a la misma `organization` del padre o a un descendiente; las fechas del hijo caen dentro del rango del padre. Se validan al crear. Validar al cambiar las fechas del padre depende de `PUT /events/{id}`, que sigue sin implementarse (deuda técnica).
8. **Sin recurrencia en la primera versión.** Ni el padre ni un hijo pueden pertenecer a una serie (`series_id`): se rechaza con `DomainValidationException`. Repetir el mega evento cada año se hace duplicándolo.
9. **Borrado.** Borrar (lógico) el padre oculta a sus hijos y sus asistencias.
10. **Calendario y Mi espacio.** El contenedor se ve como banda multi-día y sus hijos como eventos con el nombre del padre como contexto. En `me/events` y el feed `.ics` aparecen los hijos individualmente.
11. **Compartir.** No se hereda en tiempo de ejecución: al crear un hijo se copia `general_access` del padre y desde ahí el hijo se comparte por su cuenta (cada hijo conserva su `share_slug`).

## User Scenarios & Testing

### US1 — Crear un mega evento con sub-eventos (P1)

1. **Given** un `MANAGER`, **When** crea un evento del 10 al 12 y luego un curso con `parentEventId` del 11 de 9:00 a 11:00, **Then** `GET /events/{id}` del padre lista al curso en `children`.
2. **Given** un hijo con fechas fuera del rango del padre, un padre que ya es hijo, un ciclo o un nodo ajeno, **Then** 400.
3. **Given** un padre o hijo con `recurrence`, **Then** 400.

### US2 — Inscripción heredada (P1)

1. **Given** un mega evento con inscripción abierta y tres hijos `INHERIT`, **When** una persona se inscribe al mega evento, **Then** queda con un lugar en los tres hijos.
2. **Given** un hijo `OWN` con 10 lugares, **Then** la inscripción al padre no le da lugar en ese hijo y debe inscribirse aparte.
3. **Given** un hijo `OPEN`, **Then** cualquiera que pueda abrirlo asiste sin inscripción.

### US3 — Asistencia por sub-evento (P1)

1. **Given** una inscripción `CONFIRMED` al padre, **When** el ujier escanea su ticket en un hijo `INHERIT`, **Then** la persona queda presente solo en ese hijo, con origen `QR_TICKET`; repetir responde `alreadyCheckedIn`.
2. **Given** un hijo `OWN` y un ticket solo del padre, **Then** 400.

### US4 — Programa en la página pública (P2)

`GET /public/events/{slug}` del mega evento incluye el programa (hijos con día, hora y si están incluidos en la entrada o piden inscripción aparte). No expone inscritos.

### US5 — Borrar el mega evento (P2)

**Given** un padre con hijos, **When** se borra, **Then** los hijos dejan de listarse (`deleted_time`).

## Requirements

- **FR-001**: Un hijo MUST NOT tener hijos.
- **FR-002**: Las fechas de un hijo MUST caer dentro del rango del padre al crearlo.
- **FR-003**: El acceso heredado MUST resolver el registro efectivo subiendo un nivel al padre y MUST NOT dar lugar en un hijo `OWN`.
- **FR-004**: El check-in por ticket MUST ser idempotente y marcar solo el evento donde se escaneó.
- **FR-005**: La página pública MUST NOT exponer inscritos ni códigos de cancelación al mostrar el programa.
- **FR-006**: Un evento suelto sin hijos MUST comportarse como antes de esta feature.

## Success Criteria

- **SC-001**: Sin `parentEventId` nada cambia respecto de hoy (migración aditiva; `registration_mode` por defecto `INHERIT` sin efecto en eventos sin padre).
- **SC-002**: Ninguna prueba deja a una persona con lugar en un hijo `OWN` por su inscripción al padre.

## Fuera de alcance (deuda técnica, `docs/redesign.md` §10)

Recurrencia de contenedores; más de un nivel de anidación; asistencia consolidada por inscrito en todo el mega evento; cobro (Culqi); una inscripción que aplique a algunos hijos pero no a otros según el ticket (tickets por sub-evento).
