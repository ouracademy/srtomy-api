# Data Model: Eventos compuestos

Migración `V1.15.0__composite_events.sql`.

| Tabla / columna | Detalle |
| --- | --- |
| `event.parent_event_id` | `uuid` nulo, FK a `event(id)`. Nulo = evento suelto o contenedor. Solo un nivel. |
| `event.registration_mode` | `varchar(7)` no nulo, por defecto `INHERIT`, `CHECK IN ('INHERIT','OWN','OPEN')`. Solo tiene efecto en un hijo. |
| `event_parent_id_idx` | Índice en `(parent_event_id, start_time)`. |

Sin tablas nuevas: `event_registration_setup`, `ticket_type`, `event_registration` y `event_attendance` se reusan tal cual.

## Reglas

- Un hijo no tiene hijos ni `series_id`; el padre tampoco tiene `series_id`.
- Las fechas del hijo caen dentro del rango del padre.
- La organización del hijo es la del padre o un descendiente.
- Al crear un hijo se copia `general_access` del padre.
