# Tasks: Editar eventos y cursos

- [x] T001 Spec con las decisiones (`spec.md`) y plan (`plan.md`).
- [x] T002 Dominio: `Event.edit/moveTo/leaveSeries/joinSeries/hasStartedAt`, `EventSeries.rename/reschedule/successor`, `Recurrence.remainingFrom`.
- [x] T003 `attendance`: consulta `find_event_ids_with_attendance`.
- [x] T004 `event`: `EditEventCommand`/`Result`, `EditEventCommandHandlerImpl` (suelto, `THIS_EVENT`, en el lugar, regenerar, correos) y `validateSchedule` (punto de extensión).
- [x] T005 `EventController.edit` pasa los datos y devuelve `EditEventResponse`.
- [x] T006 Pruebas: handler (`EditEventCommandHandlerImplTests`), controlador (`EventControllerTests`), integración (`EditEventsIntegrationTests`).
- [x] T007 `docs/redesign.md`: quitar la deuda 16 y anotar 35 a 37.
- [x] T008 `./mvnw -B -ntp verify` y `python3 scripts/check-language.py` en verde.
- [ ] T009 UI (`srtomy-ui`, `specs/013-edit-events`): ver su `tasks.md`.
