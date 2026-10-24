# Feature Specification: Gestión de accesos y roles personalizados

**Feature Branch**: `claude/004-access-management`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "004-access-management: pestaña \"Accesos\" por nodo (asignaciones heredadas y directas, otorgar y quitar roles) y roles IAM personalizados por iglesia (crear, editar, borrar con borrado lógico, eligiendo permisos del catálogo, desde cero o desde un rol predefinido). Reglas: no se otorga un rol que uno no tiene en ese nodo o un ancestro; no se quita al último Propietario; el Administrador no otorga ni revoca Propietario."

Depende de [`003-identity-iam`](../003-identity-iam/spec.md). Diseño completo: [`docs/redesign.md`](../../docs/redesign.md).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver quién tiene acceso a un nodo (Priority: P1)

Quien tiene permiso de lectura de accesos en un nodo (iglesia, sede, grupo o ministerio) abre la pestaña "Accesos" y ve una lista de las personas con acceso: cada fila indica el usuario, el rol y si la asignación es **directa** (hecha en ese nodo) o **heredada** (hecha en un ancestro, indicando en cuál).

**Why this priority**: sin ver los accesos no se puede administrarlos ni auditarlos.

**Independent Test**: con un Propietario en la iglesia y un Líder en un grupo, abrir los accesos del grupo y ver al Propietario como heredado (de la iglesia) y al Líder como directo.

**Acceptance Scenarios**:

1. **Given** un usuario con `iam.read` en un nodo, **When** pide los accesos del nodo, **Then** recibe las asignaciones directas y las heredadas de todos sus ancestros, marcadas como tales, con el nodo de origen.
2. **Given** un usuario sin `iam.read` en el nodo, **When** pide sus accesos, **Then** recibe 403.
3. **Given** asignaciones de un rol personalizado borrado, **When** se listan los accesos, **Then** no aparecen como vigentes.

---

### User Story 2 - Otorgar y quitar roles (Priority: P1)

Quien tiene `iam.manage` en un nodo otorga a un usuario un rol sobre ese nodo, o quita una asignación directa. Las asignaciones heredadas no se quitan desde el nodo hijo, sino en el nodo donde se hicieron.

**Why this priority**: es el núcleo de la delegación de acceso.

**Independent Test**: un Administrador otorga Líder a otro usuario en un grupo, esa persona obtiene acceso al grupo y subgrupos; el Administrador luego lo quita y el acceso desaparece.

**Acceptance Scenarios**:

1. **Given** un usuario con `iam.manage` que tiene el rol R en el nodo o un ancestro, **When** otorga R a otro usuario en el nodo, **Then** se crea la asignación y queda registrado quién la otorgó.
2. **Given** un usuario que no tiene el rol R ni en el nodo ni en un ancestro, **When** intenta otorgarlo, **Then** se rechaza con 403.
3. **Given** un Administrador, **When** intenta otorgar o revocar Propietario, **Then** se rechaza con 403.
4. **Given** el único Propietario de una iglesia, **When** se intenta quitar su asignación de Propietario, **Then** se rechaza con un error de validación; si hay otro Propietario, se permite.
5. **Given** una asignación heredada, **When** se intenta quitar desde un nodo descendiente, **Then** se rechaza (no existe en ese nodo).
6. **Given** que el usuario ya tiene ese rol en ese nodo, **When** se otorga de nuevo, **Then** no se duplica.
7. **Given** un usuario inexistente o de otra iglesia sin ningún vínculo, **When** se le otorga un rol, **Then** el sistema lo identifica por su correo registrado; si no existe, se rechaza (las invitaciones llegan en la fase 005).

---

### User Story 3 - Roles personalizados por iglesia (Priority: P2)

Quien tiene `iam.manage` en la iglesia crea un rol propio eligiendo permisos de una lista agrupada por recurso, partiendo de cero o copiando un rol predefinido. Puede editar su nombre, descripción y permisos, y borrarlo. Los predefinidos no se editan ni se borran. Los roles personalizados se pueden otorgar en cualquier nodo de esa iglesia.

**Why this priority**: cada iglesia tiene necesidades distintas, pero los cinco predefinidos cubren el arranque.

**Independent Test**: crear el rol "Tesorero" a partir de Colaborador más `events.write`, otorgarlo y comprobar los permisos efectivos del usuario.

**Acceptance Scenarios**:

1. **Given** quien tiene `iam.manage` en la iglesia, **When** crea un rol con nombre y permisos válidos, **Then** el rol queda disponible solo en esa iglesia.
2. **Given** una lista de permisos con uno que no existe en el catálogo, **When** se guarda, **Then** se rechaza.
3. **Given** un nombre vacío o repetido (sin importar mayúsculas) entre los roles de la iglesia y los predefinidos, **When** se guarda, **Then** se rechaza.
4. **Given** un rol personalizado, **When** se editan sus permisos, **Then** los usuarios que lo tienen reciben de inmediato los permisos nuevos.
5. **Given** un rol predefinido, **When** se intenta editarlo o borrarlo, **Then** se rechaza.
6. **Given** un rol personalizado con asignaciones, **When** se borra, **Then** se oculta de la lista, las asignaciones dejan de valer y no se pueden otorgar nuevas.
7. **Given** un rol personalizado de la iglesia A, **When** alguien de la iglesia B intenta usarlo u otorgarlo, **Then** se rechaza.
8. **Given** quien no tiene todos los permisos de un rol, **When** intenta crear o editar un rol con más permisos que los suyos, **Then** se rechaza (sin escalada de privilegios).

---

### Edge Cases

- Al borrar un rol personalizado que es el único medio de acceso de alguien, esa persona pierde el acceso; el rol Propietario es predefinido, así que nunca se pierde por esta vía.
- Un usuario sin `iam.manage` pero con `iam.read` (Líder) ve los accesos, pero no puede cambiarlos.
- Otorgarse un rol a uno mismo está sujeto a las mismas reglas (solo roles que ya tiene).
- Quitarse a uno mismo la asignación es válido, salvo que sea el último Propietario.
- Un rol con permisos que el otorgante no posee: solo puede otorgarse si el otorgante tiene ese mismo rol en el nodo o un ancestro (FR-005).

## Requirements *(mandatory)*

### Functional Requirements

**Accesos de un nodo**

- **FR-001**: El sistema MUST listar, para un nodo, las asignaciones directas y las heredadas de sus ancestros, indicando usuario (correo), rol, nodo de origen y si es heredada. Requiere `iam.read` en el nodo.
- **FR-002**: El sistema MUST listar los roles disponibles para otorgar en un nodo: los predefinidos y los personalizados de su iglesia, sin los borrados.
- **FR-003**: El sistema MUST permitir otorgar un rol a un usuario, identificado por su correo verificado, sobre un nodo. Requiere `iam.manage` en el nodo. No duplica asignaciones.
- **FR-004**: El sistema MUST permitir quitar una asignación directa de un nodo. Requiere `iam.manage` en el nodo. Las heredadas no se quitan desde el descendiente.

**Reglas de otorgamiento**

- **FR-005**: Solo se puede otorgar un rol que el otorgante tiene (directo o heredado) en ese nodo o en un ancestro, o cuyos permisos el otorgante ya posee en ese nodo (así un Propietario puede otorgar Administrador). Propietario solo lo otorga quien es Propietario.
- **FR-006**: El sistema MUST rechazar quitar la asignación de Propietario si es la última en la iglesia.
- **FR-007**: Quien no es Propietario MUST NOT poder otorgar ni revocar el rol Propietario, aunque tenga `iam.manage`.
- **FR-008**: El sistema MUST registrar quién otorgó cada asignación y cuándo.
- **FR-009**: Asignar a alguien como líder de un grupo (accountability `LEADER`) MUST NOT crear una asignación de rol en esta fase; eso corresponde a la fase 005 (decisión del dueño del producto).

**Roles personalizados**

- **FR-010**: El sistema MUST permitir crear, editar y borrar roles personalizados de una iglesia. Requiere `iam.manage` en la iglesia.
- **FR-011**: Un rol personalizado MUST tener un nombre no vacío, único (sin importar mayúsculas) entre los roles vigentes de la iglesia y los predefinidos, y al menos un permiso.
- **FR-012**: Cada permiso de un rol MUST existir en el catálogo de permisos. El catálogo se expone agrupado por recurso para que la interfaz arme la lista de selección.
- **FR-013**: Un rol MUST poder crearse desde cero o partiendo de los permisos de un rol predefinido (la interfaz precarga la lista; el API solo recibe permisos).
- **FR-014**: Los roles predefinidos MUST NOT poder editarse ni borrarse.
- **FR-015**: Borrar un rol personalizado MUST ser lógico: se oculta, sus asignaciones dejan de valer en los permisos efectivos y no se otorga de nuevo.
- **FR-016**: Quien crea o edita un rol MUST poseer todos los permisos que le asigna (en la iglesia), salvo que sea Propietario.
- **FR-017**: Un rol personalizado solo puede otorgarse en nodos de su iglesia.

### Key Entities

- **Asignación de rol**: usuario, rol, nodo, otorgante y fecha; ahora con vista de lectura que incluye las heredadas.
- **Rol personalizado**: nombre, descripción y permisos, perteneciente a una iglesia, con borrado lógico.
- **Catálogo de permisos**: lista fija de permisos del sistema, agrupados por recurso.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un administrador otorga un rol a una persona en menos de 1 minuto desde que abre "Accesos".
- **SC-002**: El 100 % de las operaciones de accesos y roles rechazan con 403 a quien no tiene `iam.read`/`iam.manage` en el nodo, verificado con pruebas.
- **SC-003**: Ningún usuario consigue más permisos de los que tiene: otorgar roles no poseídos, otorgar Propietario siendo Administrador y crear roles con permisos ajenos se rechazan, verificado con pruebas.
- **SC-004**: Una iglesia nunca queda sin Propietario por la interfaz ni por la API.
- **SC-005**: Un cambio en los permisos de un rol personalizado se refleja en los permisos efectivos de sus usuarios en la siguiente petición.

## Assumptions

- Solo se identifica a usuarios con correo verificado, para que un correo ajeno sin verificar no reciba accesos. El usuario a quien se otorga debe haber iniciado sesión al menos una vez (existir en `app_user`) y se identifica por su correo; las invitaciones por correo a quien aún no existe son de la fase 005.
- Los permisos `iam.read` e `iam.manage` ya existen en el catálogo y los roles predefinidos de la 003 los asignan (Propietario y Administrador: ambos; Líder: `iam.read`).
- El Administrador se diferencia del Propietario solo en no poder otorgar ni revocar Propietario.
- Los roles personalizados no pueden otorgar permisos que su creador no tenga; el Propietario los tiene todos.
- La asignación automática del rol Líder al nombrar líderes de grupo queda para la 005.
