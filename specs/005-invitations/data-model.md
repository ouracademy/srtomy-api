# Data model: Invitaciones

Migración `V1.4.0__invitations.sql`.

| Tabla / columna | Detalle |
| --- | --- |
| `person.email` | `varchar(255)` nulo. |
| `iam_role.permissions` | Propietario, Administrador y Líder ganan `invitations.send`. |
| `invitation` | `id`, `email`, `organization_id` (nodo), `position_id` (cargo, nulo), `person_id` (persona existente, nulo), `first_name`/`last_name` (si no hay `person_id`), `token_hash` (único, SHA-256), `status` (`PENDING`/`ACCEPTED`/`REVOKED`), `expires_at`, `invited_by`, `accepted_by`, fechas. |

`EXPIRED` no se guarda: una `PENDING` con `expires_at` pasado se muestra como vencida (`Invitation.effectiveStatus`).

Nuevo permiso: `invitations.send`.
