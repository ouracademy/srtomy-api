# Feature Specification: Compartir eventos y cursos

**Feature Branch**: `ccr-76a49388-8zpdqw`

**Created**: 2026-10-24

**Status**: Implemented (API)

**Input**: Un evento o curso hoy lo ve solo quien tiene `events.read` en su nodo. Se comparte como en Drive: con personas por correo, con la iglesia o con quien tenga el enlace, y por el enlace de un curso la gente se inscribe. Diseño: [`docs/redesign.md`](../../docs/redesign.md) §7.

## Decisiones

1. **Acceso general** (`event.general_access`): `RESTRICTED` (por defecto), `CHURCH`, `ANYONE_WITH_LINK`. **Rol del enlace** (`event.link_role`): `VIEWER` (ve) o `REGISTRANT` (ve y se inscribe; **solo cursos**).
2. **Compartido directo** (`event_share`): por correo y con rol `VIEWER` o `EDITOR`. Aplica a quien entre con ese correo **verificado**, tenga cuenta ya o no; no se crea una invitación a la app, se le avisa por correo con el enlace.
3. **Nivel de acceso a un evento**: `MANAGER` (`events.write` en su nodo), `EDITOR` (compartido como editor), `VIEWER` (`events.read`, compartido como lector o `CHURCH` siendo de la iglesia). Gana el más alto. Compartir, ver inscritos y borrar piden `MANAGER`; editar pide `EDITOR`; ver pide `VIEWER`.
4. El enlace es `/e/{slug}` con un `slug` aleatorio de 96 bits; **renovarlo** revoca el anterior.
5. Una inscripción por el enlace **no crea una persona de la iglesia**: guarda nombre, apellido, correo y teléfono; los organizadores deciden. Un correo se inscribe una vez por evento.
6. Compartir se aplica al evento guardado: en una serie recurrente, a la serie (las ocurrencias se calculan al vuelo y no tienen identidad propia).

## User Scenarios & Testing

### US1 — Abrir un evento por el enlace (P1)

1. **Given** un evento `ANYONE_WITH_LINK`, **When** alguien sin sesión abre `GET /public/events/{slug}`, **Then** ve título, fecha, lugar, descripción y el nodo, nada más.
2. **Given** `CHURCH`, **Then** lo abre quien tiene acceso en la iglesia (con sesión); un anónimo recibe 404 y una persona de otra iglesia también.
3. **Given** `RESTRICTED`, **Then** solo lo abre quien tiene acceso por el árbol o porque se lo compartieron.

### US2 — Compartir con personas (P1)

1. **Given** un `MANAGER`, **When** comparte con `ana@x.org` como `VIEWER`, **Then** Ana (con ese correo verificado) puede ver el evento y recibe un correo con el enlace; compartir otra vez cambia el rol.
2. **Given** un correo sin verificar, **Then** el compartido no le da acceso.
3. **Given** un `VIEWER`, **Then** no administra el compartido ni borra el evento.

### US3 — Inscripción por el enlace (P1)

1. **Given** un curso con `ANYONE_WITH_LINK` y `REGISTRANT`, **When** alguien se inscribe sin cuenta, **Then** se guarda y la repetición con el mismo correo no duplica.
2. **Given** `CHURCH` + `REGISTRANT`, **Then** solo se inscribe quien es de la iglesia; **Given** `VIEWER` o `RESTRICTED`, **Then** 403.
3. **Given** un evento que no es curso, **When** se le pone `REGISTRANT`, **Then** 400.
4. Los organizadores ven la lista de inscritos (`events.write`) y pueden quitar a alguien.

### US4 — Revocar el enlace (P2)

Renovar el enlace deja sin efecto el anterior (404).

## Requirements

- **FR-001**: La página compartida MUST NOT exponer el enlace, las personas con quien se comparte ni los inscritos.
- **FR-002**: Un compartido MUST aplicar solo al correo **verificado** del usuario.
- **FR-003**: El POST de inscripción MUST ser público (sin sesión) y validar nombre, apellido y correo.
- **FR-004**: El nivel de acceso se calcula por petición (sin caché).
- **FR-005**: Los eventos existentes se migran `RESTRICTED`, con un `slug` propio.

## Success Criteria

- **SC-001**: Un evento sin compartir se comporta exactamente como antes de la fase.
- **SC-002**: Revocar un enlace o un compartido surte efecto en la siguiente petición.

## Assumptions

- Editar un evento aún no está implementado en el API; el rol `EDITOR` ya se guarda y se exige en `PUT /events/{id}`.
- Los inscritos no se vuelven personas de la iglesia solos; convertirlos es trabajo de una fase futura.
