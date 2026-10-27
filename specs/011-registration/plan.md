# Implementation Plan: Inscripción a eventos y cursos

**Spec**: [spec.md](./spec.md)

## Diseño

1. **Dominio** (`event`): `EventRegistrationSetup`, `TicketType`, `RegistrationStatus` y `EventRegistration` rehecha con ticket y estado. Se quita `LinkRole` y `Event.shareVia` solo recibe el acceso general.
2. **Reglas en el dominio**: `EventRegistrationSetup.validate` (al menos un ticket, nombres únicos, cantidades y ventana válidas) y `TicketType.isOnSale(now)`. Los cupos los decide `RegisterForEventCommandHandlerImpl` con una consulta de conteo bajo el bloqueo de la fila de la configuración (`PESSIMISTIC_WRITE`).
3. **`EventAccess`**: se quita `canRegister`; ahora «puede abrir la página» (`canOpen`) vive ahí y la usan la consulta de la página y la inscripción.
4. Comandos: `set_event_registration`, `remove_event_registration`, `register_for_event` (rehecho), `cancel_event_registration` (organizador; promueve la espera), `cancel_registration_by_code`. Consultas: `get_event_registration`, `list_event_registrations` (con ticket y estado), `get_shared_event` (con el bloque `registration`), `list_my_events` (con la inscripción).
5. Cola de espera: `WaitlistPromotion` (helper en la raíz de `command/`) lo usan las dos formas de cancelar.
6. `EventSharingController`: rutas nuevas y `SecurityConfig` abre `DELETE /v1/public/registrations/*`.

## Constitution Check

| Principio | Cómo |
| --- | --- |
| II. Tests | Pruebas unitarias de cada handler, del dominio y del controlador; integración en Postgres (cupos, espera, migración). |
| III. Seguridad | Código de cancelación de 96 bits; la página pública no trae inscritos; el cupo se cuenta bajo bloqueo. |
| IV. Código en inglés | Sí. |
| V. Simplicidad | Gratis = ticket de precio 0; sin pasarela. |

## Endpoints

Ver [contracts/http.md](./contracts/http.md).
