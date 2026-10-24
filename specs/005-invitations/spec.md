# Feature Specification: Invitaciones

**Feature Branch**: `ccr-76a49388-8zpdqw`

**Created**: 2026-10-24

**Status**: Implemented (API)

**Input**: Hasta la 004b solo el creador de una iglesia puede estar vinculado a un usuario. Para que el resto de la gente use la app, quien lidera invita por correo y, al aceptar, la persona queda vinculada a su cuenta y con su cargo. Diseño: [`docs/redesign.md`](../../docs/redesign.md) §5.

## Decisiones

1. La invitación pide **correo + cargo** (el cargo es opcional: sin cargo solo vincula la cuenta). Por eso se hizo después de la 004b.
2. Quien invita necesita `invitations.send` en el nodo y, si el cargo da acceso, **todos los permisos de ese cargo** (misma regla anti-escalada que la 004b, pero con `invitations.send` en lugar de `iam.manage`: un Líder puede invitar a su grupo).
3. Al aceptar se vuelve a comprobar que quien invitó sigue pudiendo dar ese acceso; si no, la invitación no sirve.
4. El token solo viaja en el correo; la BD guarda su SHA-256. Vence a los 14 días.
5. El correo lo envía **Resend** desde un módulo `notification`; sin clave (local, pruebas) solo se registra en el log.

## User Scenarios & Testing

### US1 — Invitar por correo con un cargo (P1)

1. **Given** un Administrador, **When** invita a `beto@x.org` con el cargo Ujier a una sede, **Then** queda una invitación PENDING y Beto recibe un correo con el enlace `/{origen}/invitacion/{token}`.
2. **Given** un Líder de un grupo, **When** invita con un cargo cuyos permisos él tiene, **Then** se crea; si el cargo tiene permisos que no tiene (p. ej. Administrador) → 403; fuera de su grupo → 403.
3. **Given** un Colaborador (sin `invitations.send`), **Then** 403.
4. **Given** un cargo de otra iglesia, **Then** 400.
5. **Given** una invitación pendiente para el mismo correo y nodo, **When** se invita otra vez, **Then** la anterior se revoca (sirve como «reenviar»).

### US2 — Aceptar vincula usuario y persona (P1)

1. **Given** el enlace, **When** el usuario inicia sesión con el correo invitado (verificado) y acepta, **Then** se crea su ficha de persona (o se usa la indicada), queda vinculada a su usuario y recibe el cargo en el nodo.
2. **Given** otro correo, o el mismo sin verificar, **Then** 403.
3. **Given** una invitación usada, revocada o vencida, **Then** 400.
4. **Given** un usuario que ya tiene ficha en la iglesia, **Then** conserva esa ficha y se suma al nodo; si la invitación nombraba a otra persona → 400.

### US3 — Invitar líderes (P2)

Desde el formulario de un grupo o ministerio, al elegir líderes que tienen correo, una casilla «Invitar a usar la app» (marcada) invita a cada uno con su ficha (`personId`). Al aceptar, la acreditación `LEADER` ya existente les da el nivel Líder en el grupo (004b).

### US4 — Gestionar invitaciones (P2)

Quien tiene `invitations.send` lista las pendientes de un nodo (las vencidas salen marcadas) y puede cancelarlas. Un usuario sin iglesias ve «Tienes invitaciones» con las de su correo verificado y las acepta.

## Requirements

- **FR-001**: `person.email` (opcional, formato de correo) se guarda, se devuelve y se edita.
- **FR-002**: `POST /organizations/{id}/invitations` MUST exigir `invitations.send` y, para un cargo con acceso, todos sus permisos.
- **FR-003**: El token MUST NOT devolverse por la API ni guardarse en claro.
- **FR-004**: Aceptar MUST exigir que el correo **verificado** del usuario sea el de la invitación.
- **FR-005**: Una persona ya vinculada a un usuario MUST NOT poder ser invitada.
- **FR-006**: `GET /public/invitations/{token}` (sin sesión) MUST mostrar solo correo, nodo, cargo y estado.
- **FR-007**: Si el correo no se pudo enviar, la invitación MUST conservarse y la respuesta avisarlo (`emailSent=false`).
- **FR-008**: `invitations.send` se agrega a Propietario, Administrador y Líder.

## Success Criteria

- **SC-001**: Una persona recibe el correo, inicia sesión y en un paso tiene su cuenta vinculada y su cargo.
- **SC-002**: Ningún usuario puede dar, por invitación, más acceso del que tiene.

## Assumptions

- Auth0 debe agregar `email` y `email_verified` al token (ver README); sin eso nadie puede aceptar.
- Los cargos existentes conservan sus permisos tal como están; solo los roles predefinidos ganan `invitations.send`.
