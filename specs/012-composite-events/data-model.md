# Data Model: Eventos compuestos

Migración `V1.18.0__composite_events.sql`.

| Tabla / columna | Detalle |
| --- | --- |
| `event.parent_event_id` | `uuid` nulo, FK a `event(id)`. Nulo = evento suelto o contenedor. Solo un nivel. |
| `event.registration_mode` | `varchar(7)` no nulo, por defecto `INHERIT`, `CHECK IN ('INHERIT','OWN','OPEN')`. Solo tiene efecto en un hijo. |
| `event_parent_id_idx` | Índice en `(parent_event_id, start_time)`. |

`event_registration_setup`, `ticket_type`, `event_registration` y `event_attendance` se reusan tal cual.

## Ingreso a un sub-evento que hereda

Migración `V1.19.0__sub_event_check_in.sql`.

| Tabla / columna | Detalle |
| --- | --- |
| `event_registration_check_in` | Una fila por (inscripción del mega evento, sub-evento `INHERIT`) cuando esa persona entró al sub-evento, tenga o no cuenta. No toca `event_registration.checked_in_at`, que sigue significando «entró al mega evento». |
| `registration_id` | FK a `event_registration`, `ON DELETE CASCADE`. |
| `event_id` | FK al sub-evento. |
| `checked_in_at`, `check_in_source` | Cuándo y cómo (`MANUAL`, `QR_EVENT`, `QR_TICKET`). |
| `event_registration_check_in_key` | Única por `(registration_id, event_id)`: marcar dos veces conserva la primera hora. |

## Reglas

- Un hijo no tiene hijos ni `series_id`; el padre tampoco tiene `series_id`.
- Las fechas del hijo caen dentro del rango del padre.
- La organización del hijo es la del padre o un descendiente.
- Al crear un hijo se copia `general_access` del padre.
