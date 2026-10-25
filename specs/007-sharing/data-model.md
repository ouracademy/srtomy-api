# Data model: Compartir

Migración `V1.6.0__sharing.sql`.

| Tabla / columna | Detalle |
| --- | --- |
| `event.general_access` | `RESTRICTED` (por defecto), `CHURCH`, `ANYONE_WITH_LINK` (CHECK). |
| `event.link_role` | `VIEWER` (por defecto), `REGISTRANT` (CHECK). |
| `event.share_slug` | Único; 20 hex en los eventos existentes, 16 URL-safe aleatorios en los nuevos. |
| `event_share` | `id`, `event_id`, `email`, `role` (`VIEWER`/`EDITOR`), `shared_by`; único `(event_id, lower(email))`. |
| `event_registration` | `id`, `event_id`, `user_id` (nulo), `first_name`, `last_name`, `email`, `phone_number`; único `(event_id, lower(email))`. |
