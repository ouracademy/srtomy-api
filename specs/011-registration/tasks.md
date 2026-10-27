# Tasks: Inscripción a eventos y cursos

- [ ] T001 Migración `V1.10.0__registration.sql`, entidades, ids tipados y repositorios.
- [ ] T002 Quitar `LinkRole` de `Event`, `EventAccess`, el compartir y los DTO.
- [ ] T003 Comandos `set_event_registration`, `remove_event_registration`.
- [ ] T004 `register_for_event` con ticket, cupos, espera y ventana de venta.
- [ ] T005 `cancel_event_registration` y `cancel_registration_by_code` con promoción desde la espera.
- [ ] T006 Consultas `get_event_registration`, `list_event_registrations`, `get_shared_event`, `list_my_events`.
- [ ] T007 `EventSharingController`, `SecurityConfig` y DTO.
- [ ] T008 Pruebas unitarias por handler y de integración en Postgres.
- [ ] T009 Docs: `docs/redesign.md` §7 y deuda técnica.
- [ ] T010 `mvn test`, `spotless:check` y `scripts/check-language.py`.

## Asistencia por QR

- [ ] T011 Migración de asistencia: `ticket_code`, `checked_in_at`, `check_in_source`, `guest_count`, `person.church_status`, origen en `person_event_attendance`.
- [ ] T012 `AttendanceQrToken` (firma, ventana, expiración) y su configuración.
- [ ] T013 `generate_attendance_qr`, `mark_self_attendance` (cuenta, visitante con nombre, anónimo) con idempotencia.
- [ ] T014 `check_in_with_ticket`, `set_registration_check_in`, `set_attendance_guests`, `add_visitor` y el estado de la persona (`set_church_status`, sube a `MEMBER` con un cargo; `user_access` excluye a `VISITOR`).
- [ ] T015 `GET /me/events` con `ticketCode`/`checkedIn` y «Asistí» unificado.
- [ ] T016 Pruebas unitarias por handler y de integración (token vencido, alterado, doble escaneo, espera/cancelada, sin permiso).

## Preguntas abiertas para el visto bueno

- (Resuelto) El QR se ofrece como QR del evento y como QR del ticket, y la asistencia admite visitantes sin nombre (contador) y con nombre.
- ¿Aceptas que quien puede abrir la página (incluido `CHURCH`) pueda inscribirse, o la inscripción debe poder abrirse a gente que no ve el evento?
- El correo de confirmación con el enlace de cancelación queda como deuda técnica (outbox); mientras tanto el código se muestra al inscribirse y en Mi espacio.
- Quitar un ticket con historial de inscripciones se rechaza; quitar toda la inscripción borra las filas canceladas.
- El rename `srtomy` → `ovejafiel` (PR #50) ya está en `main`: los paquetes de este plan son `org.ouracademy.ovejafiel.*`; los repos siguen llamándose `srtomy-api` y `srtomy-ui`.
