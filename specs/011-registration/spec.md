# Feature Specification: Inscripción a eventos y cursos

**Feature Branch**: `claude/tender-ritchie-fu1afp`

**Created**: 2026-10-27

**Status**: Plan (sin implementar). Pendiente de visto bueno del dueño del producto.

**Input**: El dueño del producto revisó la 007 y la juzgó mal planteada: inscribirse era un *rol del enlace* (`REGISTRANT`, solo cursos) y solo guardaba nombre, correo y teléfono. Ahora la **inscripción es un proceso aparte** del compartir, como en Eventbrite o Meetup: el evento define si es gratis o tiene tipos de ticket, cupos y una ventana de venta. Diseño: [`docs/redesign.md`](../../docs/redesign.md) §7.

## Decisiones (acordadas con el dueño del producto)

1. **Compartir y inscribirse son cosas distintas.** Compartir (007) dice **quién puede abrir** el evento (`RESTRICTED`, `CHURCH`, `ANYONE_WITH_LINK`). Inscribirse dice **quién tiene un lugar**. Se quita `event.link_role` y el rol `REGISTRANT`: el enlace solo deja ver, así que ya no hay un selector de un único valor. Quien puede abrir la página de un evento con inscripción abierta puede inscribirse.
2. **Inscripción opcional.** Un evento o curso nace **sin inscripción** (`event_registration_setup` no existe). Los organizadores (`events.write`) la activan con una configuración: abierta o cerrada, si exige cuenta, si hay lista de espera, un cupo total opcional y **uno o más tipos de ticket**.
3. **Gratis es un ticket de precio 0.** «Gratis» en la UI crea un único ticket «General». No hay modo aparte en el modelo.
4. **Tipo de ticket** (`ticket_type`): nombre, precio (`price_cents`, moneda de 3 letras, por defecto `PEN`), cantidad opcional (nula = sin límite) y ventana de venta opcional (`sales_start`, `sales_end`).
5. **Pagos fuera de alcance.** El precio es informativo: no se cobra ni se integra una pasarela. Una fase futura agrega un estado de pago sin cambiar este modelo.
6. **Inscripción** (`event_registration`): pertenece a un tipo de ticket y tiene estado `CONFIRMED`, `WAITLISTED` o `CANCELLED`. Una persona (por correo) tiene como máximo una inscripción activa (`CONFIRMED` o `WAITLISTED`) por evento. Cancelar conserva la fila (historial).
7. **Cupos.** Un ticket está lleno al llegar a su cantidad y el evento al llegar al cupo total (cuentan solo las `CONFIRMED`). Si hay lista de espera, quien llega con todo lleno queda `WAITLISTED`; si no, se rechaza con 400. Al cancelar una confirmada, entra la primera de la espera (por orden de llegada) a la que le quepa. Los cupos se cuentan bajo un bloqueo de la configuración del evento, para no pasarse con inscripciones simultáneas.
8. **Cancelar.** Cada inscripción tiene un `cancel_code` aleatorio (96 bits). Quien se inscribe lo recibe en la respuesta y, con cuenta, en `GET /me/events`; con él cancela sin sesión (`DELETE /public/registrations/{code}`). Los organizadores también pueden quitar a alguien.
9. **Datos que se piden:** nombre, apellido, correo y teléfono opcional. Las preguntas propias por evento quedan como trabajo futuro.
10. **Datos anteriores.** El dueño pidió **reemplazar y borrar**: la migración elimina las inscripciones de la 007 y la columna `link_role` (no se edita ninguna migración aplicada).
11. **Mi espacio.** «Mis eventos y cursos» = lo que tengo con un lugar (inscripción confirmada o en espera, por mi cuenta o por mi correo verificado) **y** lo que asistí. «Compartidos contigo» = lo que otra persona o la iglesia me dejó ver y todavía no es mío por inscripción ni asistencia. Cada tarjeta dice por qué aparece.

12. **Compartir sin selectores de una sola opción (UI).** «Acceso general» queda en un select (Restringido / Toda la iglesia / Cualquiera con el enlace); el rol por persona (Lector/Editor) se mantiene. Se quita «Con el enlace». Un select de rol solo aparece si tiene dos valores con sentido.
13. **Guardar explícito (UI).** El diálogo Compartir **nunca** guarda solo: acumula los cambios (personas agregadas o quitadas, acceso general) y un botón **Guardar**, deshabilitado hasta que algo cambie y con spinner mientras dura la petición (patrón `api-submit-button`), los aplica con los endpoints existentes. Sin cambios en el API para esto salvo quitar `linkRole`.
14. **Crear/editar un evento** gana una sección «Inscripción»: Sin inscripción (por defecto), Gratis (cupo total opcional) o Con tipos de ticket. La UI guarda el evento y luego `PUT /events/{id}/registration`.

## User Scenarios & Testing

### US1 — Activar la inscripción (P1)

1. **Given** un `MANAGER`, **When** guarda una configuración con dos tickets (uno de precio 0, uno de 2000 con 20 lugares), **Then** `GET /events/{id}/registration` la devuelve con los vendidos de cada uno.
2. **Given** una configuración sin tickets, nombres repetidos o cantidades < 1, **Then** 400.
3. **Given** inscripciones activas en un ticket, **When** se quita ese ticket o se desactiva la inscripción, **Then** 400 (hay que cerrarla en su lugar).
4. **Given** un `VIEWER`, **Then** 403 al configurar.

### US2 — Inscribirse desde la página pública (P1)

1. **Given** una inscripción abierta, **When** un visitante que puede abrir la página elige un ticket y envía sus datos, **Then** queda `CONFIRMED` y recibe su código de cancelación; repetir con el mismo correo no duplica (`alreadyRegistered`).
2. **Given** la inscripción cerrada, un ticket fuera de su ventana o un evento sin inscripción, **Then** 400.
3. **Given** que exige cuenta, **When** no hay sesión, **Then** 403.
4. **Given** un evento al que el visitante no puede acceder (`RESTRICTED`), **Then** 404 al abrir la página y 403 al inscribirse.

### US3 — Cupos y lista de espera (P1)

1. **Given** un ticket con 1 lugar y sin espera, **When** se inscribe una segunda persona, **Then** 400 «se agotaron».
2. **Given** lo mismo con espera, **Then** queda `WAITLISTED`; **When** la primera cancela, **Then** la segunda pasa a `CONFIRMED`.
3. **Given** un cupo total menor que la suma de los tickets, **Then** el total manda.

### US4 — Administrar inscritos (P1)

Los organizadores ven los inscritos con ticket y estado (`GET /events/{id}/registrations`) y pueden quitar a alguien (pasa a `CANCELLED` y se llena el lugar desde la espera).

### US5 — Mi espacio (P1)

`GET /me/events` devuelve inscripciones activas y asistencias con una marca por cada origen, y por cada inscripción su ticket, estado y código de cancelación. Una inscripción cancelada no cuenta como «registrado».

## Requirements

- **FR-001**: La página pública MUST NOT exponer inscritos ni códigos de cancelación de otros.
- **FR-002**: Las inscripciones simultáneas MUST NOT superar un cupo.
- **FR-003**: El POST de inscripción MUST ser público y validar nombre, apellido, correo y ticket.
- **FR-004**: Una inscripción por una cuenta con correo verificado MUST contar como propia aunque se hiciera sin sesión con ese correo.
- **FR-005**: Compartir (`PUT /events/{id}/sharing`) MUST NOT recibir ni devolver `linkRole`.

## Success Criteria

- **SC-001**: Un evento sin configuración se comporta como antes, salvo que ya no hay inscripción por enlace.
- **SC-002**: Ninguna prueba de cupos deja más confirmadas que el cupo.

## Fuera de alcance (deuda técnica, `docs/redesign.md` §10)

Pagos reales; correo de confirmación con el enlace de cancelación y aviso al pasar de la espera a confirmada (outbox); preguntas propias por evento; varios boletos por compra; convertir inscritos en personas de la iglesia (ítem 17); cancelar con un plazo.
