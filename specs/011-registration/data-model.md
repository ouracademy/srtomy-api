# Data model: Inscripción

Migración `V1.10.0__registration.sql` (hacia adelante; no toca migraciones aplicadas).

| Tabla / columna | Detalle |
| --- | --- |
| `event.link_role` | **Se elimina** (borra también el valor `REGISTRANT`). |
| `event_registration` (007) | **Se elimina** y se vuelve a crear con la forma de abajo (el dueño pidió no migrar sus filas). |
| `event_registration_setup` | `id`, `event_id` (único), `open`, `requires_account`, `waitlist_enabled`, `capacity` (nulo = sin límite, `> 0`). Ausente = el evento no tiene inscripción. |
| `ticket_type` | `id`, `event_id`, `name` (único por evento sin importar mayúsculas), `price_cents` (`>= 0`), `currency` (3 letras), `quantity` (nulo = sin límite, `> 0`), `sales_start`, `sales_end` (`sales_start < sales_end` si ambos), `position`. |
| `event_registration` | `id`, `event_id`, `ticket_type_id`, `user_id` (nulo), `first_name`, `last_name`, `email`, `phone_number`, `status` (`CONFIRMED`/`WAITLISTED`/`CANCELLED`), `cancel_code` (único). Único parcial `(event_id, lower(email))` donde `status <> 'CANCELLED'`. |
