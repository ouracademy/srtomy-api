# Data model: Inscripción

Migración `V1.11.0__registration.sql` (hacia adelante; no toca migraciones aplicadas; la `V1.10.0` ya era del outbox).

| Tabla / columna | Detalle |
| --- | --- |
| `event.link_role` | **Se elimina** (borra también el valor `REGISTRANT`). |
| `event_registration` (007) | **Se elimina** y se vuelve a crear con la forma de abajo (el dueño pidió no migrar sus filas). |
| `event_registration_setup` | `id`, `event_id` (único), `open`, `requires_account` (los cupos son la `quantity` de cada ticket). Ausente = el evento no tiene inscripción. |
| `ticket_type` | `id`, `event_id`, `name` (único por evento sin importar mayúsculas), `price_cents` (`>= 0`), `currency` (3 letras), `quantity` (nulo = sin límite, `> 0`), `sales_start`, `sales_end` (`sales_start < sales_end` si ambos), `position`. |
| `event_registration` | `id`, `event_id`, `ticket_type_id`, `user_id` (nulo), `first_name`, `last_name`, `email`, `phone_number`, `status` (`CONFIRMED`/`CANCELLED`), `cancel_code` (único). Único parcial `(event_id, lower(email))` donde `status <> 'CANCELLED'`. |

## Asistencia por QR

Migración `V1.12.0__attendance_qr.sql`. Los campos del ingreso del ticket (`ticket_code`, `checked_in_at`, `check_in_source`) ya van en la tabla `event_registration` de la `V1.11.0`.

| Tabla / columna | Detalle |
| --- | --- |
| `event_registration.ticket_code` | Único, aleatorio (96 bits); es el contenido del QR del ticket (distinto de `cancel_code`). |
| `event_registration.checked_in_at`, `check_in_source` | Nulos hasta que ingresa; `check_in_source` en `MANUAL`/`QR_EVENT`/`QR_TICKET`. |
| `event_attendance.guest_count` | Entero `>= 0`: visitantes sin nombre. |
| `person_event_attendance.source`, `marked_by` | Origen de la marca (por defecto `MANUAL`) y quién la hizo (nulo si se marcó solo). |

El estado de la persona (visitante, asistente, miembro) **no agrega tablas ni columnas**: son las relaciones `VISITOR`, `MEMBERSHIP_IN_PROGRESS` y `MEMBER` que la persona ya tiene con su nodo.

El código del QR del evento no se guarda: es `HMAC-SHA256` de `eventId` y la ventana de 5 minutos con el secreto `attendance.qr-secret`.
