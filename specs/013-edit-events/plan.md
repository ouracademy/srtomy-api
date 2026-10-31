# Implementation Plan: Editar eventos y cursos

**Spec**: [spec.md](./spec.md)

## Diseño

1. **Contrato**: `EditEventRequest` (ya existía: `EventRequest` + `option`) y `EditEventResponse {id, keptEvents, notifiedPeople}` reemplazan el `UUID` de `PUT /events/{id}`. `option` ausente = `THIS_EVENT`. Cambia el contrato: en `srtomy-ui` se corre `pnpm api:sync`.
2. **Comando**: `EditEventCommand` lleva todos los datos (nombre, horas, lugar, descripción, `isACourse` y `organizationId` solo para rechazar cambios, ponentes, `recurrence`, `timeZone` y `now`). `now` lo pone el controlador (igual que `ExtendEventSeriesCommand.today`) para que las pruebas no dependan del reloj.
3. **Dominio**: `Event.edit/moveTo/leaveSeries/joinSeries/hasStartedAt`; `EventSeries.rename/reschedule/successor` (la serie que toma el relevo conserva el estado de materialización para que el job de extensión siga); `Recurrence.remainingFrom` (un `COUNT` conserva solo las repeticiones que faltan al partir una serie).
4. **Handler** (`EditEventCommandHandlerImpl`, una transacción): valida (nodo y tipo no cambian, fin no anterior al inicio, ponentes existentes) y elige el camino:
   - evento suelto → se edita en el lugar; con `recurrence` se vuelve la primera ocurrencia de una serie nueva;
   - `THIS_EVENT` → se edita y sale de la serie;
   - `THIS_AND_THE_FOLLOWING_EVENTS`/`ALL_EVENTS` → **en el lugar** si no cambió regla, zona ni día; **regenerar** si cambió alguna (ver spec, decisión 4).
   Después envía un correo por persona (`SendEmailCommandHandler`, tipo `EVENT_CHANGED`) con lo que cambió (hora, nombre, lugar o ponentes) y las fechas de sus ocurrencias.
5. **Cruce de módulos**: asistencia se consulta con `attendance.query.find_event_ids_with_attendance` (nuevo, devuelve solo ids con alguien presente o visitantes contados); la asistencia de lo que se reemplaza se borra con `DeleteAttendanceForEventsCommandHandler`, como en borrar. Las inscripciones están en el propio módulo (`EventRegistrationRepository.findByEventIdInAndStatusNot`).
6. **Punto de extensión**: `validateSchedule(event, edit)` en el handler, vacío, para los eventos compuestos (spec 012).
7. Sin migración: ninguna columna nueva.

## Constitution Check

| Principio | Cómo |
| --- | --- |
| II. Tests | Unitarias del handler y del controlador; integración en Postgres (`EditEventsIntegrationTests`). |
| III. Seguridad | Sigue pidiendo `EDITOR` sobre el evento; no se puede cambiar el nodo, así que no se escala a otra iglesia. |
| IV. Código en inglés | Sí; los mensajes y el correo, en español. |
| V. Simplicidad | Sin migración ni librerías nuevas; los correos usan el outbox existente. |
