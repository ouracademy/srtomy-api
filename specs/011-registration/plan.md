# Implementation Plan: Inscripción a eventos y cursos

**Spec**: [spec.md](./spec.md)

## Diseño

1. **Dominio** (`event`): `EventRegistrationSetup`, `TicketType`, `RegistrationStatus` y `EventRegistration` rehecha con ticket y estado. Se quita `LinkRole` y `Event.shareVia` solo recibe el acceso general.
2. **Reglas en el dominio**: `EventRegistrationSetup.validate` (al menos un ticket, nombres únicos, cantidades y ventana válidas) y `TicketType.isOnSale(now)`. Los cupos los decide `RegisterForEventCommandHandlerImpl` con una consulta de conteo bajo el bloqueo de la fila de la configuración (`PESSIMISTIC_WRITE`).
3. **`EventAccess`**: se quita `canRegister`; ahora «puede abrir la página» (`canOpen`) vive ahí y la usan la consulta de la página y la inscripción.
4. Comandos: `set_event_registration`, `remove_event_registration`, `register_for_event` (rehecho), `cancel_event_registration` (organizador), `cancel_registration_by_code`. Consultas: `get_event_registration`, `list_event_registrations` (con ticket y estado), `get_shared_event` (con el bloque `registration`), `list_my_events` (con la inscripción).
5. No hay lista de espera (`V1.14.0`): cancelar solo libera el lugar.
6. `EventSharingController`: rutas nuevas y `SecurityConfig` abre `DELETE /v1/public/registrations/*`.

## Asistencia por QR

7. **Dominio**: `CheckInSource` (en `event`); `EventAttendance` gana `guestCount` y `markPresent`; `PersonEventAttendance` gana `source` y `markedBy`; `setPeople` conserva el origen de quienes siguen. En `event`: `EventRegistration` gana `ticketCode`, `checkedInAt`, `checkInSource`.
8. **Token del QR**: `AttendanceQrToken` (HMAC-SHA256 de `eventId` y la ventana de 5 minutos, comparación en tiempo constante, cuenta la ventana vigente y la anterior). El secreto es `attendance.qr-secret` (Secret Manager en prod, generado por Terraform; valor fijo en local y pruebas). Sin secreto no se puede emitir ni verificar.
9. **Comandos** (`attendance`): `mark_person_present`, `mark_user_present` (la persona vinculada a una cuenta en la iglesia del evento), `set_attendance_guests`, `add_visitor` (crea la persona `VISITOR` con `create_person` y la marca), `mark_self_attendance` (el escaneo del QR). **Consultas**: `get_attendance_qr`, `get_attendance_welcome`. En `event`: `check_in_registration_by_ticket`, `set_registration_check_in`, `check_in_user_registration`. Los módulos se hablan solo por handlers.
10. **Estado de la persona**: `set_member_state` (en `accountability`) deja un único estado por persona y nodo; `create_person` y `update_person` aceptan `memberState` y fuerzan `MEMBER` con cargos.
11. `AttendanceCheckInController` (rutas públicas del QR y de marcado) y `SecurityConfig` abre `POST /v1/public/attendance/*`. El contador de visitantes sin nombre suma con un `UPDATE` atómico, no leyendo y escribiendo.

## Constitution Check

| Principio | Cómo |
| --- | --- |
| II. Tests | Pruebas unitarias de cada handler, del dominio y del controlador; integración en Postgres (cupos, espera, migración). |
| III. Seguridad | Código de cancelación de 96 bits; la página pública no trae inscritos; el cupo se cuenta bajo bloqueo. |
| IV. Código en inglés | Sí. |
| V. Simplicidad | Gratis = ticket de precio 0; sin pasarela. |

## Endpoints

Ver [contracts/http.md](./contracts/http.md).
