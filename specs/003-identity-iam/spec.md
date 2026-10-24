# Feature Specification: Identidad de usuario e IAM jerárquico

**Feature Branch**: `claude/app-flow-redesign-nq6j60`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "003-identity-iam: Identidad de usuario e IAM jerárquico estilo GCP. Un usuario se registra, crea una o varias iglesias. Crear una iglesia exige su primera sede, de forma atómica, y copia cargos ministeriales sugeridos (editables, por iglesia, sin lista cerrada). Jerarquía recursiva iglesia > sede > grupo/ministerio (anidables). Roles de acceso predefinidos (Propietario, Administrador, Líder, Colaborador, Miembro) con permisos heredados a los descendientes, verificados en todos los endpoints. Enlaces sociales opcionales. Se elimina la iglesia de prueba, los cargos globales y la escritura del orgId en el proveedor de identidad."

Diseño completo y fases siguientes: [`docs/redesign.md`](../../docs/redesign.md).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Crear mi iglesia con su primera sede (Priority: P1)

Una persona se registra en la app. Como aún no pertenece a ninguna iglesia, crea una: da su nombre, opcionalmente sus enlaces sociales y la primera sede, que es obligatoria. Revisa los cargos ministeriales sugeridos (Pastor, Líder de alabanza, Servidor, Maestro), quita o renombra los que quiera y guarda. Queda como Propietaria de esa iglesia.

**Why this priority**: sin esto nadie puede empezar a usar la app. Hoy existe una iglesia de prueba y un único vínculo usuario–iglesia guardado en el proveedor de identidad.

**Independent Test**: con un usuario nuevo, crear una iglesia con su sede y comprobar que aparece en "mis iglesias", con la sede como hija, los cargos elegidos y el rol Propietario.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado sin iglesias, **When** crea una iglesia con nombre, una sede y cargos, **Then** se crean la iglesia, la sede y los cargos, y el usuario recibe el rol Propietario sobre la iglesia.
2. **Given** una solicitud de creación sin sede, **When** se envía, **Then** se rechaza y no se crea nada.
3. **Given** que falla algún paso (por ejemplo, un enlace social inválido), **When** se envía, **Then** no queda ninguna iglesia, sede ni cargo a medias.
4. **Given** un usuario que ya es Propietario de una iglesia, **When** crea otra, **Then** ve ambas en "mis iglesias".

---

### User Story 2 - Acceso según rol y herencia por la jerarquía (Priority: P1)

Cada acción de la app exige un permiso sobre un nodo de la jerarquía: iglesia, sede, grupo o ministerio. Un rol otorgado en un nodo vale para ese nodo y para todos sus descendientes. Un usuario sin rol en un nodo ni en ninguno de sus ancestros no puede ver ni modificar nada de él.

**Why this priority**: hoy cualquier usuario autenticado puede leer y modificar los datos de cualquier iglesia, incluidos datos personales de menores.

**Independent Test**: con dos usuarios y dos iglesias, verificar que cada uno solo accede a la suya, y que un Líder de un grupo accede a ese grupo y a sus subgrupos pero no a su sede.

**Acceptance Scenarios**:

1. **Given** un usuario con rol Administrador en la iglesia A, **When** lista las personas de una sede de A, **Then** las obtiene.
2. **Given** el mismo usuario, **When** intenta leer o modificar algo de la iglesia B, **Then** recibe "prohibido" (403), sin datos.
3. **Given** un usuario con rol Líder en el grupo G de la sede S, **When** edita G o crea un subgrupo de G, **Then** puede hacerlo. **When** edita S, **Then** recibe 403.
4. **Given** un usuario con rol Miembro en la iglesia, **When** consulta la información básica de una sede, **Then** la obtiene. **When** intenta crear una persona o un evento, **Then** recibe 403.
5. **Given** un recurso que no existe, **When** lo pide alguien sin acceso, **Then** la respuesta no revela si existe (403 o 404 de forma consistente).

---

### User Story 3 - Saber quién soy y dónde tengo acceso (Priority: P1)

Al entrar, la app pregunta quién es el usuario, en qué iglesias y nodos tiene acceso, y qué puede hacer en el nodo actual, para armar el selector de contexto y mostrar solo las acciones permitidas.

**Why this priority**: es lo que reemplaza el vínculo único usuario–iglesia del proveedor de identidad y habilita varias iglesias por usuario.

**Independent Test**: con un usuario con roles en dos iglesias, pedir su perfil, sus nodos y sus permisos en un nodo, y comparar con los roles otorgados.

**Acceptance Scenarios**:

1. **Given** un usuario que entra por primera vez, **When** pide su perfil, **Then** se registra con su identificador y correo, y la respuesta indica que no tiene iglesias.
2. **Given** un usuario con roles en varias iglesias, **When** pide sus nodos, **Then** recibe cada iglesia con su árbol de nodos accesibles (los nodos donde tiene rol, sus ancestros y sus descendientes), cada uno con su tipo y su padre.
3. **Given** un nodo, **When** el usuario pide sus permisos efectivos en él, **Then** recibe la unión de los permisos de los roles que tiene en ese nodo y en sus ancestros.

---

### User Story 4 - Jerarquía recursiva de grupos y ministerios (Priority: P2)

Un grupo o ministerio puede tener subgrupos o subministerios, sin límite de profundidad. Una sede cuelga siempre de una iglesia, y una iglesia no tiene padre.

**Why this priority**: las iglesias tienen estructuras de distinta profundidad y la app no debe imponer una.

**Independent Test**: crear un ministerio bajo una sede, un grupo bajo ese ministerio y otro grupo bajo ese grupo; verificar sus padres y que un rol en la sede alcanza al nieto.

**Acceptance Scenarios**:

1. **Given** un ministerio M, **When** se crea un grupo con padre M, **Then** se crea y aparece como hijo de M.
2. **Given** una iglesia, **When** se intenta crear un grupo directamente bajo la iglesia, o una sede bajo una sede, **Then** se rechaza con un error de validación.

---

### User Story 5 - Cargos ministeriales flexibles (Priority: P2)

Cada iglesia administra sus propios cargos ministeriales: las etiquetas con que nombra a su gente (Pastor, Diácono, Catequista, Asistente, Hermano servidor…). Al asignar un cargo, la app sugiere los de la iglesia y un catálogo de nombres comunes; si escribe uno nuevo, se crea. Los cargos no otorgan permisos.

**Why this priority**: distintas tradiciones nombran distinto a sus servidores. Hoy hay cargos fijos en inglés que se traducen en la interfaz.

**Independent Test**: renombrar "Servidor" a "Ujier", crear "Catequista", asignarlo a una persona y verificar que no cambia ningún permiso.

**Acceptance Scenarios**:

1. **Given** una iglesia con el cargo "Servidor", **When** un Administrador lo renombra a "Ujier", **Then** las personas que lo tenían ahora muestran "Ujier".
2. **Given** un cargo con el mismo nombre (sin importar mayúsculas) ya existente, **When** se crea otro igual, **Then** se rechaza.
3. **Given** un cargo asignado, **When** se borra, **Then** deja de ofrecerse, y las asignaciones dejan de mostrarse sin perder el historial.
4. **When** se pide el catálogo de sugerencias, **Then** se recibe una lista de nombres comunes de distintas tradiciones.

---

### Edge Cases

- Un usuario sin correo en su token. Se registra igual; el correo queda vacío y se completa en la siguiente entrada que lo traiga.
- Se intenta borrar una iglesia o sede. No se permite en esta fase, como hoy; solo se borran grupos y ministerios, con `org.delete`.
- Una persona con su id en la URL pertenece a otra iglesia. Se responde 403 aunque el usuario tenga acceso a su propia iglesia.
- Un evento cuyo nodo es un subgrupo profundo. Lo pueden ver quienes tienen el permiso en cualquier ancestro.
- La base de datos ya tenía la iglesia de prueba y datos de prueba. La migración los elimina.

## Requirements *(mandatory)*

### Functional Requirements

**Identidad**

- **FR-001**: El sistema MUST registrar a cada usuario que se autentica por primera vez, con su identificador del proveedor de identidad y, si viene, su correo y si está verificado. En entradas posteriores MUST actualizar el correo.
- **FR-002**: El sistema MUST distinguir usuario (quien inicia sesión) de persona (registro de la iglesia). Un usuario puede quedar vinculado a una persona, pero no es obligatorio.
- **FR-003**: El sistema MUST dejar de escribir el id de la iglesia en el proveedor de identidad. Las iglesias de un usuario se derivan solo de sus roles.

**Jerarquía**

- **FR-004**: El sistema MUST validar el tipo del padre al crear nodos: iglesia sin padre; sede bajo iglesia; grupo o ministerio bajo sede, grupo o ministerio.
- **FR-005**: El sistema MUST permitir anidar grupos y ministerios sin límite de profundidad.
- **FR-006**: El sistema MUST permitir resolver, para cualquier nodo, su iglesia y su cadena de ancestros, sin depender de la profundidad.

**Creación de iglesia**

- **FR-007**: El sistema MUST crear, en una sola operación todo-o-nada, la iglesia, su primera sede, sus cargos iniciales y el rol Propietario del creador.
- **FR-008**: El creador MUST ser siempre el usuario autenticado. El sistema no acepta un identificador de usuario enviado por el cliente.
- **FR-009**: Iglesias, sedes, grupos y ministerios MUST admitir enlaces opcionales: sitio web, Facebook, Instagram, YouTube y TikTok. Si vienen, MUST ser URLs http(s) válidas.

**Roles de acceso y permisos**

- **FR-010**: El sistema MUST definir estos permisos: `org.read`, `org.update`, `org.create_child`, `org.delete`, `members.read`, `members.write`, `events.read`, `events.write`, `attendance.write`, `positions.manage`, `iam.read` e `iam.manage`. `invitations.send` se reserva para la fase de invitaciones.
- **FR-011**: El sistema MUST ofrecer los roles predefinidos Propietario, Administrador, Líder, Colaborador y Miembro, con los permisos de la tabla en Assumptions. No son editables.
- **FR-012**: Un rol otorgado a un usuario en un nodo MUST aplicar a ese nodo y a todos sus descendientes.
- **FR-013**: Todo endpoint que lee o modifica datos de una iglesia MUST verificar el permiso correspondiente sobre el nodo afectado. Si el endpoint recibe un evento o una persona, el nodo se resuelve desde ese recurso. Si no hay permiso, MUST responder 403.
- **FR-014**: El sistema MUST exponer el perfil del usuario actual, sus nodos accesibles agrupados por iglesia y sus permisos efectivos en un nodo dado.
- **FR-015**: Las búsquedas y listados (personas, eventos, hijos de un nodo) MUST devolver solo datos de nodos donde el usuario tiene el permiso de lectura correspondiente.

**Cargos ministeriales**

- **FR-016**: Los cargos MUST pertenecer a una iglesia. Dentro de la iglesia, el nombre MUST ser no vacío y único sin importar mayúsculas. No hay lista cerrada.
- **FR-017**: Quien tenga `positions.manage` en la iglesia MUST poder listar, crear, renombrar y borrar cargos (el borrado es lógico).
- **FR-018**: El sistema MUST ofrecer un catálogo estático de nombres sugeridos para autocompletar, que incluya términos de distintas tradiciones.
- **FR-019**: Al asignar cargos a una persona, un nombre que aún no existe en la iglesia MUST crearse como cargo nuevo.
- **FR-020**: Los cargos MUST NOT otorgar permisos.

**Limpieza**

- **FR-021**: La migración MUST eliminar la iglesia de prueba, todos los datos de dominio de prueba y los cargos globales (Pastor, Worship leader, Usher). MUST conservar los tipos de relación del sistema (miembro, visitante, forma parte de, líder, etc.).

### Key Entities

- **Usuario**: quien inicia sesión. Tiene identificador externo único, correo y si está verificado. Puede tener una persona vinculada.
- **Nodo (organización)**: iglesia, sede, grupo o ministerio. Tiene un padre (salvo la iglesia), una cadena de ancestros y enlaces sociales opcionales.
- **Permiso**: una acción sobre un tipo de recurso (`recurso.acción`).
- **Rol de acceso**: un nombre y un conjunto de permisos. Los predefinidos no tienen iglesia. Los personalizados (fase 004) pertenecen a una iglesia.
- **Asignación de rol**: un usuario, un rol y un nodo. Registra quién la otorgó y cuándo.
- **Cargo ministerial**: una etiqueta de la iglesia que se asigna a personas en una sede, grupo o ministerio. No da permisos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario nuevo puede tener su iglesia y su primera sede creadas en menos de 2 minutos desde que inicia sesión.
- **SC-002**: El 100 % de los endpoints de datos de iglesia rechazan con 403 a un usuario sin el permiso correspondiente. Lo verifica una prueba por endpoint, o por grupo de endpoints con la misma regla.
- **SC-003**: Un rol otorgado en la iglesia alcanza a un nodo a 5 niveles de profundidad, y un rol en un grupo no alcanza a su padre. Ambos casos se verifican con pruebas.
- **SC-004**: Un mismo usuario puede tener acceso a 2 o más iglesias y cambiar entre ellas sin volver a iniciar sesión.
- **SC-005**: No queda ninguna iglesia ni cargo precargado en una base nueva.

## Assumptions

- Permisos de los roles predefinidos:

  | Rol           | Permisos                                                                                                                                              |
  | ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
  | Propietario   | todos                                                                                                                                                 |
  | Administrador | todos (se diferencia del Propietario en la fase 004: no puede quitar ni otorgar Propietario)                                                          |
  | Líder         | `org.read`, `org.update`, `org.create_child`, `members.read`, `members.write`, `events.read`, `events.write`, `attendance.write`, `iam.read`           |
  | Colaborador   | `org.read`, `members.read`, `events.read`, `attendance.write`                                                                                         |
  | Miembro       | `org.read`, `events.read`                                                                                                                             |

- Otorgar y quitar roles por la interfaz, los roles personalizados y las invitaciones son de las fases 004 y 005. En esta fase solo se otorga Propietario al crear la iglesia. Las pruebas otorgan roles directamente.
- La asignación de líderes de un grupo no crea aún un rol de acceso; eso llega con las invitaciones (fase 005).
- El correo del usuario llega como claim del token de acceso, agregado por una regla del proveedor de identidad. Si falta, el usuario se registra sin correo.
- No hay datos reales en ningún entorno, así que se pueden borrar todos los datos de dominio.
- Cargos sugeridos al crear una iglesia: Pastor, Líder de alabanza, Servidor y Maestro.
