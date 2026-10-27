# Tasks: Inscripción a eventos y cursos

- [x] T001 Migración `V1.11.0__registration.sql`, entidades, ids tipados y repositorios.
- [x] T002 Quitar `LinkRole` de `Event`, `EventAccess`, el compartir y los DTO.
- [x] T003 Comandos `set_event_registration`, `remove_event_registration`.
- [x] T004 `register_for_event` con ticket, cupos, espera y ventana de venta.
- [x] T005 `cancel_event_registration` y `cancel_registration_by_code` con promoción desde la espera.
- [x] T006 Consultas `get_event_registration`, `list_event_registrations`, `get_shared_event`, `list_my_events`.
- [x] T007 `EventSharingController`, `SecurityConfig` y DTO.
- [x] T008 Pruebas unitarias por handler y de integración en Postgres.
- [x] T009 Docs: `docs/redesign.md` §7 y deuda técnica.
- [x] T010 `mvn test`, `spotless:check` y `scripts/check-language.py`.

## Asistencia por QR

- [x] T011 Migración de asistencia (`V1.12.0`): `ticket_code`, `checked_in_at`, `check_in_source`, `guest_count`, origen en `person_event_attendance`.
- [x] T012 `AttendanceQrToken` (firma, ventana, expiración) y su configuración.
- [x] T013 `generate_attendance_qr`, `mark_self_attendance` (cuenta, visitante con nombre, anónimo) con idempotencia.
- [x] T014 `check_in_registration_by_ticket`, `set_registration_check_in`, `set_attendance_guests`, `add_visitor` y el estado de la persona (`set_member_state`, sube a `MEMBER` con un cargo).
- [x] T015 `GET /me/events` con `ticketCode`/`checkedIn` y «Asistí» unificado.
- [x] T016 Pruebas unitarias por handler y de integración (token vencido, alterado, doble escaneo, espera/cancelada, sin permiso).

## Decisiones tomadas con el dueño del producto

- Quien puede abrir la página de un evento (incluido `CHURCH`) puede inscribirse; la inscripción no abre el evento a quien no lo ve.
- El correo de confirmación con el enlace de cancelación queda como deuda técnica (outbox); mientras tanto el código se muestra al inscribirse y en Mi espacio.
- Quitar un ticket con historial de inscripciones se rechaza; quitar toda la inscripción exige que nadie tenga lugar y borra las filas canceladas.
- El QR se ofrece como QR del evento y como QR del ticket; la asistencia admite visitantes con nombre (personas con estado) y sin nombre (contador).
- El rename `srtomy` → `ovejafiel` (PR #50) ya está en `main`: los paquetes son `org.ouracademy.ovejafiel.*`; los repos siguen llamándose `srtomy-api` y `srtomy-ui`.
