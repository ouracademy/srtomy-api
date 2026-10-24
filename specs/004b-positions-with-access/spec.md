# Feature Specification: Cargos con nivel de acceso

**Feature Branch**: `claude/positions-with-access`

**Created**: 2026-10-24

**Status**: Draft

**Input**: Quien administra una iglesia piensa "si es pastor debe tener estos accesos, si es servidor estos otros". Hoy el **cargo** (etiqueta de una persona) y el **rol** (permisos de un usuario) son dos cosas sin conexión, y dar acceso exige otorgar un rol aparte por correo. Se unifican: el cargo lleva un nivel de acceso. Diseño: [`docs/redesign.md`](../../docs/redesign.md) §3.

## Decisiones tomadas con el dueño del producto

1. Todo acceso nace de un cargo; no hay "otorgar un rol suelto por correo". La única excepción es **Propietario**, que se gestiona aparte.
2. El creador de la iglesia declara su propio cargo en el alta y se crea su ficha de persona vinculada a su usuario. Sigue siendo Propietario por separado.
3. Esta fase va antes de las invitaciones (005).

## Revisión de diseño (mismo día, con el dueño del producto)

Un solo "nivel" por cargo no alcanzaba. El modelo final, como los grupos de GCP:
- Un cargo da la **unión** de los permisos de sus roles y de sus permisos sueltos. En la UI solo se ven y se marcan **permisos** agrupados por módulo; los roles quedan en el modelo para una UI futura.
- Los cargos predefinidos (plantillas) nacen **copiando** los permisos de roles predefinidos; no quedan vinculados al catálogo.
- Donde este documento dice "nivel de acceso" léase "permisos del cargo (roles y/o sueltos)".

## User Scenarios & Testing

### US1 — Un cargo da acceso según su nivel (P1)

Cada cargo de una iglesia tiene un **nivel de acceso** opcional: Administrador, Líder, Colaborador, Miembro o un nivel propio de la iglesia. Sin nivel es solo una etiqueta, para personas que no usan la app. Si la persona que tiene el cargo está vinculada a un usuario, ese usuario obtiene los permisos del nivel **en el nodo donde tiene el cargo y en todo lo que cuelga de él**.

1. **Given** el cargo Pastor con nivel Administrador y una persona vinculada que lo tiene en la sede S, **When** pide sus permisos en S o en un grupo de S, **Then** tiene los del Administrador.
2. **Given** la misma persona, **When** pide permisos en otra sede, **Then** no tiene nada.
3. **Given** un cargo sin nivel, **When** una persona vinculada lo tiene, **Then** no obtiene permisos.
4. **Given** una persona **sin** usuario vinculado con el cargo Pastor, **Then** nadie obtiene acceso por ella.
5. **Given** que se le quita el cargo a la persona, **Then** el usuario pierde de inmediato los permisos.
6. **Given** que se cambia el nivel del cargo, **Then** cambia de inmediato el acceso de todos los que lo tienen.

### US2 — Alta de iglesia con niveles y con mi cargo (P1)

Al crear la iglesia, cada cargo sugerido nace con un nivel genérico editable (Pastor → Administrador, Líder de alabanza → Líder, Servidor → Colaborador, Maestro → Colaborador). El creador indica su nombre y su cargo; se crea su ficha de persona en la primera sede, vinculada a su usuario.

1. **Given** un alta con cargos y niveles y el cargo del creador, **When** se crea, **Then** existen la iglesia, la sede, los cargos con su nivel, la ficha del creador con su cargo y su vínculo, y el creador es Propietario.
2. **Given** un nivel inexistente o Propietario en un cargo, **When** se crea, **Then** se rechaza y no se crea nada.

### US3 — Administrar el nivel de cada cargo (P1)

Quien tiene `iam.manage` puede crear cargos con nivel, cambiarlo o quitarlo. Los cargos muestran cuántas personas los tienen. Un nivel propio en uso por algún cargo no se puede borrar.

### US4 — Sin escalada de privilegios (P1)

1. **Given** un Líder (con `members.write`), **When** intenta asignar a alguien un cargo que da acceso, **Then** se rechaza (403): hace falta `iam.manage` en ese nodo.
2. **Given** alguien con `iam.manage` pero sin todos los permisos del nivel, **When** asigna o cambia un cargo con ese nivel, **Then** se rechaza.
3. **Given** cualquier usuario, **When** intenta dar a un cargo el nivel Propietario, **Then** se rechaza.
4. Asignar cargos sin nivel solo exige `members.write`, como hoy.

### US5 — Quién tiene acceso (P2)

La pantalla de accesos de un nodo lista a las personas con acceso: el nivel y el origen de cada una (cargo y nodo, "Líder del grupo" o "Propietario"), heredado o directo. Los Propietarios se agregan y quitan aparte, solo por un Propietario, y la iglesia conserva al menos uno.

### US6 — Líderes de grupo (P2)

Quien figura como líder (`LEADER`) de un grupo o ministerio y tiene usuario vinculado obtiene el nivel Líder en ese grupo (y sus subgrupos).

## Requirements

- **FR-001**: Un cargo MUST poder tener un nivel de acceso (`iam_role`) o ninguno. El nivel nunca es Propietario.
- **FR-002**: Los permisos efectivos de un usuario MUST salir de: sus asignaciones directas (Propietario), los cargos de las personas vinculadas a él, y el liderazgo de grupos, siempre en el nodo y sus ancestros.
- **FR-003**: Un usuario MUST poder estar vinculado a **una persona por iglesia**; una persona, a un usuario. (Un usuario puede pertenecer a varias iglesias.)
- **FR-004**: Asignar, quitar, crear, cambiar o borrar un cargo con nivel MUST exigir `iam.manage` y que quien lo hace tenga todos los permisos de ese nivel (el actual y el nuevo al cambiarlo).
- **FR-005**: El alta MUST aceptar el nivel de cada cargo y el cargo del creador, y crear su ficha y vínculo, todo o nada.
- **FR-006**: `GET /positions/suggestions` MUST devolver cada nombre con su nivel sugerido.
- **FR-007**: Los cargos MUST informar cuántas personas los tienen.
- **FR-008**: El API MUST NOT permitir otorgar o quitar niveles sueltos; solo la gestión de Propietarios.
- **FR-009**: Borrar un nivel propio que algún cargo usa MUST rechazarse.
- **FR-010**: Se MUST conservar las reglas de la 004: solo un Propietario otorga o quita Propietario, y la iglesia conserva al menos uno.

## Success Criteria

- **SC-001**: Una iglesia nueva queda con sus cargos ya conectados a un nivel de acceso, sin tocar nada más.
- **SC-002**: Cambiar el nivel de un cargo cambia el acceso de todos sus titulares en la siguiente petición.
- **SC-003**: Un Líder no puede, por ningún endpoint, dar a otra persona más acceso del que tiene.

## Assumptions

- Hasta la fase 005 solo el creador de cada iglesia puede estar vinculado a un usuario (la invitación aceptada vinculará al resto). Es el estado esperado, no un error.
- No hay datos reales: la migración borra las asignaciones directas que no sean de Propietario.
- Sin cargo, una persona con cuenta no tiene acceso; el nivel Miembro "por pertenecer" queda para la fase 008 (deuda 009).
