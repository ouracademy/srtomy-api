# Tasks: Eventos compuestos

## Datos y dominio
- [x] T001 Migración `V1.18.0__composite_events.sql`.
- [x] T002 `RegistrationMode`, `Event.parent`, `Event.registrationMode`, `Event.attachTo` y pruebas.
- [x] T003 `EventRepository.findByParentOrderByStart` y `EventFixtures.childOf`.

## Crear y borrar
- [x] T004 `parentEventId` y `registrationMode` en `EventRequest`, `CreateEventCommand` y `EventController`.
- [x] T005 `CreateEventCommandHandlerImpl` con padre (organización, recurrencia, `attachTo`) y pruebas.
- [x] T006 `DeleteEventCommandHandlerImpl` borra los hijos y pruebas.

## Leer
- [x] T007 `FindEventResult` y `EventResponse` con padre, modo y `children`.

## Inscripción y asistencia
- [x] T008 `EffectiveRegistration` y pruebas.
- [x] T009 Rechazar la inscripción en un hijo `INHERIT` u `OPEN`.
- [x] T010 Check-in por ticket heredado (y `OWN` sin ticket propio → 400).

## Pública y Mi espacio
- [x] T011 `program` en la página pública.
- [x] T012 Hijos heredados en `me/events` y título con el padre en el feed `.ics`.

## Edición (013)
- [x] T016 Con la edición de eventos: rangos de fechas, sin repetición, sin cambiar de mega evento, `registrationMode` y aviso a los inscritos del mega evento (`EditEventCommandHandlerImpl`), con pruebas de handler e integración.

## Cierre
- [x] T013 Prueba de integración `CompositeEventsIntegrationTests`.
- [x] T014 `docs/redesign.md` §7 y §10.
- [x] T015 `./mvnw spotless:apply`, `./mvnw verify` y `python3 scripts/check-language.py`.

## Decisiones tomadas con el dueño del producto

Un nivel; inscripción heredada con `INHERIT`/`OWN`/`OPEN`; el mega evento se ve como banda de varios días. Compartir se copia al crear, no se hereda.
