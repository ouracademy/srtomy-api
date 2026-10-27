# Implementation Plan: Inscripción a eventos y cursos

**Spec**: [spec.md](./spec.md)

## Diseño

1. **Dominio** (`event`): `EventRegistrationSetup`, `TicketType`, `RegistrationStatus` y `EventRegistration` rehecha con ticket y estado. Se quita `LinkRole` y `Event.shareVia` solo recibe el acceso general.
2. **Reglas en el dominio**: `EventRegistrationSetup.validate` (al menos un ticket, nombres únicos, cantidades y ventana válidas) y `TicketType.isOnSale(now)`. Los cupos los decide `RegisterForEventCommandHandlerImpl` con una consulta de conteo bajo el bloqueo de la fila de la configuración (`PESSIMISTIC_WRITE`).
3. **`EventAccess`**: se quita `canRegister`; ahora «puede abrir la página» (`canOpen`) vive ahí y la usan la consulta de la página y la inscripción.
4. Comandos: `set_event_registration`, `remove_event_registration`, `register_for_event` (rehecho), `cancel_event_registration` (organizador; promueve la espera), `cancel_registration_by_code`. Consultas: `get_event_registration`, `list_event_registrations` (con ticket y estado), `get_shared_event` (con el bloque `registration`), `list_my_events` (con la inscripción).
5. Cola de espera: `WaitlistPromotion` (helper en la raíz de `command/`) lo usan las dos formas de cancelar.
6. `EventSharingController`: rutas nuevas y `SecurityConfig` abre `DELETE /v1/public/registrations/*`.

## Asistencia por QR

7. **Dominio** (`attendance`): `AttendanceSource`, `AttendanceGuest`; `EventAttendance` gana `guestCount`; `PersonEventAttendance` gana `source` y `markedBy`. En `event`: `EventRegistration` gana `ticketCode`, `checkedInAt`, `checkInSource`.
8. **Token del QR**: `AttendanceQrToken` (HMAC-SHA256 de `eventId` y la ventana de 5 minutos; comparación en tiempo constante). El secreto se lee de `attendance.qr-secret` (Secret Manager en prod; valor fijo en pruebas).
9. Comandos: `generate_attendance_qr` (consulta con ventana), `mark_self_attendance`, `check_in_with_ticket`, `set_registration_check_in`, `set_attendance_guests`. La unión inscripción↔persona usa `FindPersonLinkedToUser` del módulo `person`/`user` (sin tocar sus repositorios).
10. `SecurityConfig` abre `GET`/`POST /v1/public/attendance/*`. Límite de peticiones por IP en el endpoint público (si no existe, deuda técnica).

## Constitution Check

| Principio | Cómo |
| --- | --- |
| II. Tests | Pruebas unitarias de cada handler, del dominio y del controlador; integración en Postgres (cupos, espera, migración). |
| III. Seguridad | Código de cancelación de 96 bits; la página pública no trae inscritos; el cupo se cuenta bajo bloqueo. |
| IV. Código en inglés | Sí. |
| V. Simplicidad | Gratis = ticket de precio 0; sin pasarela. |

## Endpoints

Ver [contracts/http.md](./contracts/http.md).
