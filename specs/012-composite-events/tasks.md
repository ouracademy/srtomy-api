# Tasks: Eventos compuestos

## Datos y dominio
- [x] T001 Migración `V1.15.0__composite_events.sql`.
- [x] T002 `RegistrationMode`, `Event.parent`, `Event.registrationMode`, `Event.attachTo` y pruebas.
- [x] T003 `EventRepository.findByParentOrderByStart` y `EventFixtures.childOf`.

## Crear y borrar
- [x] T004 `parentEventId` y `registrationMode` en `EventRequest`, `CreateEventCommand` y `EventController`.
- [x] T005 `CreateEventCommandHandlerImpl` con padre (organización, recurrencia, `attachTo`) y pruebas.
- [x] T006 `DeleteEventCommandHandlerImpl` borra los hijos y pruebas.

## Leer
- [ ] T007 `FindEventResult` y `EventResponse` con padre, modo y `children`.

## Inscripción y asistencia
- [ ] T008 `EffectiveRegistration` y pruebas.
- [ ] T009 Rechazar la inscripción en un hijo `INHERIT` u `OPEN`.
- [ ] T010 Check-in por ticket heredado (y `OWN` sin ticket propio → 400).

## Pública y Mi espacio
- [ ] T011 `program` en la página pública.
- [ ] T012 Hijos heredados en `me/events` y título con el padre en el feed `.ics`.

## Cierre
- [ ] T013 Prueba de integración `CompositeEventsIntegrationTests`.
- [ ] T014 `docs/redesign.md` §7 y §10.
- [ ] T015 `./mvnw spotless:apply`, `./mvnw verify` y `python3 scripts/check-language.py`.

## Decisiones tomadas con el dueño del producto

Un nivel; inscripción heredada con `INHERIT`/`OWN`/`OPEN`; el mega evento se ve como banda de varios días. Compartir se copia al crear, no se hereda.
