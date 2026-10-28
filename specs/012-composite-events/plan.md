# Implementation Plan: Eventos compuestos

Spec: [`spec.md`](./spec.md). Modelo: [`data-model.md`](./data-model.md). HTTP: [`contracts/http.md`](./contracts/http.md).

## Diseño

1. **Datos.** Migración `V1.15.0`: `event.parent_event_id` (FK a `event`, con índice) y `event.registration_mode` (`INHERIT`, `OWN`, `OPEN`; por defecto `INHERIT`). Aditiva: sin `parent_event_id` nada cambia.
2. **Dominio** (`event/domain/Event.java`). `@ManyToOne Event parent`, enum `RegistrationMode` y `Event.attachTo(parent)`, que valida las reglas de forma (padre sin padre ni serie, hijo sin serie, fechas dentro del rango del padre) y copia `general_access` del padre. Compartir **no se hereda en tiempo de ejecución**: se copia al crear y luego el hijo se comparte por su cuenta.
3. **Crear.** `CreateEventCommandHandlerImpl` carga el padre, valida que la organización sea la misma o descendiente (`FindOrganizationSubtreeQueryHandler`), rechaza `recurrence` y llama a `attachTo`.
4. **Borrar.** `DeleteEventCommandHandlerImpl` borra también los hijos y su asistencia. Borrar un hijo no toca al padre.
5. **Leer.** `parentEventId`, `parentTitle`, `registrationMode` en las respuestas; `children` solo en `GET /events/{id}`. La búsqueda plana (calendario) no anida: el padre sale como banda porque solapa con el rango.
6. **Inscripción efectiva.** `EffectiveRegistration` resuelve qué evento rige la inscripción de otro: el propio (`OWN` o sin padre), el padre (`INHERIT`) o ninguno (`OPEN`). Inscribirse por el slug de un hijo `INHERIT` u `OPEN` se rechaza con un mensaje claro. `Seats` no cambia.
7. **Check-in.** El ticket del padre vale en un hijo `INHERIT`; en un hijo `OWN` sin ticket propio es 400. La asistencia se marca en el hijo y no se toca `checked_in_at` de la inscripción del padre.
8. **Pública y Mi espacio.** `program` en la página pública; en `me/events` los hijos `INHERIT` de una inscripción `CONFIRMED` al padre aparecen con el ticket del padre.
9. **Editar** (`PUT /events/{id}`) sigue sin implementarse en el API; validar los hijos al cambiar fechas del padre queda como deuda técnica.

## Constitution Check

| Principio | Cumple |
| --- | --- |
| II. Módulos | Todo en el módulo `event`; el subárbol de organizaciones se consulta por su query handler. |
| III. Handlers | Cada caso de uso es un handler con `handle(...)` que devuelve un `*Result`. |
| IV. Pruebas | Unitarias por handler, de controlador y de integración. |
| V. Migraciones | Una migración nueva; ninguna editada. |

## Endpoints

Ver [`contracts/http.md`](./contracts/http.md).
