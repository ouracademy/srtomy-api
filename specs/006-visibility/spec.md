# Feature Specification: Visibilidad

**Feature Branch**: `ccr-76a49388-8zpdqw`

**Created**: 2026-10-24

**Status**: Implemented (API)

**Input**: Hoy la gente de un grupo solo la ve quien administra. Cada iglesia, sede, grupo o ministerio decide quién ve a su gente, la iglesia tiene un directorio y cada nodo puede tener una página pública. Diseño: [`docs/redesign.md`](../../docs/redesign.md) §6.

## Decisiones

1. `organization.member_visibility` (por defecto `PUBLIC`) controla la **lista de personas**. La **información básica** del nodo (nombre, descripción, horario, líderes) está siempre en el directorio de la iglesia.
2. Las páginas públicas son un enlace con un `slug` aleatorio de 16 caracteres (96 bits); no hay listado ni buscador.
3. **Nunca se nombra a un menor de 18 años.** En la página pública solo se nombra a quien se **sabe** adulto (edad o fecha de nacimiento registradas); dentro de la iglesia se omite solo a quien se sabe menor. La fecha de nacimiento manda sobre la edad guardada.
4. Solo nombre y apellido: nunca edad, fecha de nacimiento, teléfono, correo ni dirección de una persona.
5. Se agrega `schedule` (horario) al nodo; es parte de la información básica.

## User Scenarios & Testing

### US1 — Elegir quién ve a la gente de un nodo (P1)

`memberVisibility`: `PUBLIC` (cualquiera con el enlace), `CHURCH` (cualquier usuario con acceso en la iglesia), `MEMBERS` (los miembros del nodo y quien tenga `members.read` allí), `LEADERS` (solo quien tenga `members.read`). Se edita junto con el nodo (`org.update`).

1. **Given** un grupo `MEMBERS`, **When** lo mira alguien de la iglesia que no es de ese grupo ni lo administra, **Then** `membersVisible=false` y no recibe la lista, pero sí la información básica.
2. **Given** el mismo grupo, **When** lo mira alguien de ese grupo (aunque su cargo solo lea lo básico), **Then** ve la lista.
3. **Given** un grupo `LEADERS`, **Then** solo ven la lista quienes tienen `members.read`, aunque sean miembros.

### US2 — Directorio de la iglesia (P1)

`GET /organizations/{iglesia}/directory` lista todos los nodos (también los que ocultan su gente) con nombre, tipo, padre, descripción, horario, visibilidad y líderes. `GET /organizations/{nodo}/directory-entry` da un nodo con sus hijos y, si corresponde, su gente. Quien no tiene acceso en la iglesia recibe 403.

### US3 — Página pública (P1)

`GET /public/organizations/{slug}` (sin sesión) solo si el nodo es `PUBLIC`; si no, 404.

1. **Grupo o ministerio**: información básica, **líderes destacados** y miembros, solo adultos conocidos.
2. **Iglesia o sede**: información básica con dirección y teléfono del lugar, cargos ministeriales con quién los tiene y, si `showOrgChart` está activo (por defecto), el **organigrama**: sedes, ministerios y grupos con sus líderes. Se omiten los nodos `LEADERS` y todo lo que cuelga de ellos; los nodos `PUBLIC` enlazan a su propia página.

## Requirements

- **FR-001**: Todo nodo nuevo nace `PUBLIC`, con organigrama y con un `slug` aleatorio; los existentes se migran igual.
- **FR-002**: El API MUST NOT devolver edad, fecha de nacimiento, teléfono, correo ni dirección de personas en el directorio ni en las páginas públicas.
- **FR-003**: Editar un nodo sin enviar `memberVisibility` ni `showOrgChart` los conserva; el horario se sobrescribe.
- **FR-004**: Cambiar la visibilidad MUST exigir `org.update` en el nodo (como cualquier edición).
- **FR-005**: El directorio y la regla de `MEMBERS` se calculan por petición: quitar el acceso a alguien lo saca de inmediato.

## Success Criteria

- **SC-001**: Un menor de 18 no aparece por nombre en ninguna página pública.
- **SC-002**: Un nodo `LEADERS` no aparece en el organigrama público ni expone su gente a nadie sin `members.read`.

## Assumptions

- La vista «Mi espacio» y el nivel Miembro sin cargo siguen en la 008: hoy el directorio exige tener acceso en la iglesia (un cargo o ser Propietario).
- Una persona sin edad registrada no sale en una página pública: la iglesia debe registrar la edad o la fecha de nacimiento de quien quiera mostrar.
