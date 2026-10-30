# Rediseño del flujo: iglesias multi-tenant, jerarquía recursiva, IAM estilo GCP y compartir estilo Drive

## Contexto

Hoy:

- **API** (`srtomy-api`): `V1.0.0__initial_schema.sql` siembra una iglesia falsa `LCDP` (`bb8d09a1-…`). No hay usuarios en BD: la única unión Auth0↔iglesia es `app_metadata.orgId` escrito por `Auth0Gateway` al crear la iglesia (y el `userId` llega en el body, con un TODO). **No hay autorización**: cualquier token válido puede hacer todo (`SecurityConfig`: "authenticated = full access").
- La jerarquía ya es genérica: `organization.type ∈ {CHURCH, LOCATION, GROUP, MINISTRY}` unida por filas `accountability` de tipo `IS_PART_OF`. Los niveles no se validan, y un grafo de accountabilities no sirve bien para heredar permisos.
- Los "roles" que existen (PASTOR, WORSHIP_LEADER, USHER y personalizados por sede) son **cargos ministeriales**, no permisos.
- **UI** (`srtomy-ui`): un usuario tiene una sola iglesia (claim `https://sr-tommy.com/orgId`). El claim queda viejo tras crear la iglesia, así que la sede nueva puede recibir `parentId` undefined. La sede se elige a mano y no se recuerda. No hay controles por permiso, ni ministerios, ni invitaciones, ni visibilidad, ni compartir.

Objetivo: cualquiera se registra y crea una o varias iglesias (cada una con al menos una sede). Puede cambiar de iglesia, sede, grupo o ministerio. Hay accesos por roles heredables, como IAM de GCP, y los líderes pueden invitar por correo. El miembro común tiene su propia vista. Grupos, ministerios y sedes tienen visibilidad configurable. Eventos y cursos se comparten por enlace, como en Drive.

---

## 1. Modelo de recursos (como la jerarquía de GCP)

| GCP            | Sr. Tommy                                    | Reglas                                                                                   |
| -------------- | -------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Organization   | **Iglesia** (`CHURCH`)                       | Raíz. Al crearla se exige al menos una sede.                                             |
| Folder         | **Sede** (`LOCATION`)                        | Hija directa de una iglesia.                                                             |
| Folder/Project | **Grupo / Ministerio** (`GROUP`, `MINISTRY`) | Hijo de una sede o de otro grupo o ministerio. **Recursivo, sin límite de profundidad.** |
| Resource       | Evento / curso, persona                      | Pertenece a un nodo y hereda su acceso.                                                  |

Cambios de esquema (Flyway, nueva migración `V1.1.0__...`):

- `organization.parent_id uuid null` (FK a `organization`). Los ancestros y descendientes se recorren con un CTE recursivo (`WITH RECURSIVE`). Se descartó una ruta materializada (`path uuid[]`) por simplicidad, ya que los árboles son pequeños.
- Se rellenan desde las `accountability IS_PART_OF` actuales entre organizaciones. Esas filas ya no se usan para el árbol. Para _persona ↔ organización_ (membresía, LEADER, cargos, estados) se sigue usando `accountability`.
- Validar padre/tipo en `OrganizationWriter.create`: CHURCH sin padre; LOCATION bajo CHURCH; GROUP o MINISTRY bajo LOCATION, GROUP o MINISTRY.
- **Quitar la iglesia falsa `LCDP`**. El usuario confirmó que es solo data de prueba: se elimina del seed y una migración la borra de la BD. Los 8 accountability types del sistema se quedan (los usa el código).
- Enlaces sociales opcionales en `organization`: `website`, `facebook_url`, `instagram_url`, `youtube_url`, `tiktok_url`. Se validan como URL. En los formularios de iglesia y sede (y de grupo o ministerio) aparecen en una sección plegable **"Otras opciones"**, y se muestran en la página pública.

## 2. Identidad: usuario ≠ persona

- Tabla nueva `app_user`: `id`, `auth0_sub` (único), `email`, `email_verified`. Se crea o actualiza con un upsert en `GET /v1/me`.
- Una **persona** es un registro de la iglesia y la mayoría nunca usará la app. Un **usuario** es quien inicia sesión. Se vinculan en la tabla `user_person` (una persona por usuario en cada iglesia, un usuario por persona): al crear la iglesia, con la ficha de quien la crea, o cuando la persona acepta una invitación.
- Una Auth0 Action agrega `email` y `email_verified` al access token. Se **elimina** el claim `orgId` y la escritura a `app_metadata`. La lista de iglesias del usuario sale de la BD, no del token. Así se resuelven el claim viejo y el límite de una sola iglesia.
- `OrganizationRequest.userId` desaparece: el creador es siempre el principal del JWT.

## 3. IAM: roles, permisos y bindings con herencia

Se separan dos conceptos que hoy se mezclan:

- **Cargo ministerial**: el nombre que la iglesia usa para su gente. Es un `accountability_type` categoría POSITION. Desde la **004b el cargo da permisos**: la unión de los de sus roles (`position_role`) y los sueltos (`position_permission`), así que hacia fuera hay un solo concepto: _si es pastor tiene estos accesos_. En la UI solo se marcan permisos por módulo; los roles quedan en el modelo para una UI futura. Sin permisos es solo una etiqueta (para quien no usa la app). Los cargos sugeridos nacen copiando los permisos de roles predefinidos. Debe ser flexible para cualquier tradición (evangélica, católica, etc.):
  - Los cargos pasan a pertenecer a la **iglesia** (`party_id = church_id`). Se eliminan los cargos globales con `party_id = null` (PASTOR, WORSHIP_LEADER, USHER) y su traducción fija en la UI (`Roles.tsx`, `toSpanish`, `getFixedOptions`).
  - Al crear la iglesia se **copian cargos sugeridos**: Pastor, Líder de alabanza, Servidor/Ujier y Maestro. El asistente los muestra y deja quitarlos o renombrarlos antes de guardar.
  - Después, quien tenga `org.update` en la iglesia puede **renombrar, crear o borrar** cargos en "Cargos". Borrar es un soft delete (`deleted_time`), así que las asignaciones existentes se conservan en el historial.
  - Validación mínima: solo nombre no vacío y único por iglesia (sin importar mayúsculas). Ninguna lista cerrada.
  - Al asignar un cargo a una persona hay un **autocompletado** que sugiere los cargos de la iglesia más un catálogo estático de sugerencias (Pastor, Copastor, Anciano, Diácono, Sacerdote, Catequista, Asistente, Hermano/a servidor, …). Si se escribe un nombre nuevo, se crea el cargo en la iglesia (`freeSolo`).
  - `requireOwnRole` deja de exigir al menos un cargo por sede.
  - `GET /v1/organizations/locations/predefined-roles` se reemplaza por `GET /v1/churches/{id}/positions`. El catálogo de sugerencias vive en el código del API y se expone en `GET /v1/positions/suggestions`.
- **Rol IAM**: un conjunto de permisos. Ya no se otorga suelto por correo: lo da un cargo. Un usuario recibe los permisos del cargo que tiene la persona vinculada a él, en el nodo donde lo tiene y en todo lo que cuelga. **Propietario** es la excepción: no se asigna a un cargo, es un acceso directo (`role_binding`) de quien crea la iglesia y de quienes un Propietario agrega. Antes de la 004b: un conjunto de permisos, asignado a un usuario sobre un nodo. **Se hereda a todos los descendientes**, igual que en GCP. Los predefinidos no se editan; cada iglesia puede crear **roles personalizados** eligiendo permisos (ver abajo).

Tablas:

- `iam_role` (`id`, `name`, `description`, `church_id` null = predefinido, `permissions text[]`, `deleted_time`).
  - **Roles personalizados**, como en GCP: quien tiene `iam.manage` en la iglesia crea, edita o borra roles. Puede partir de cero o de "Crear a partir de" un rol predefinido, y marca permisos en una lista agrupada por recurso.
  - El API valida que cada permiso exista en el catálogo de permisos del código.
  - Un rol con bindings no se borra en duro: se le hace soft delete y sus bindings dejan de valer.
- `role_binding` (`id`, `user_id`, `role_id`, `organization_id`, `granted_by`, `created_time`): desde la 004b solo guarda Propietarios.
- `position_role` y `position_permission` (004b): lo que da un cargo (roles vinculados y permisos sueltos). Nunca incluye el rol Propietario.
- `user_person` (004b): vincula usuario y persona por iglesia.
- Vista `user_access` (004b): junta los bindings directos, los cargos de la persona vinculada y el liderazgo de grupos. Todas las consultas de permisos leen esta vista.

Permisos (strings, estilo GCP `recurso.acción`):
`org.read`, `org.update`, `org.create_child`, `org.delete`, `members.read`, `members.write`, `events.read`, `events.write`, `attendance.write`, `registrations.read`, `registrations.configure`, `registrations.close`, `registrations.remove`, `iam.read`, `iam.manage`, `invitations.send`.

Roles predefinidos:

| Rol               | Pensado para                      | Permisos                                                                                                                   |
| ----------------- | --------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| **Propietario**   | Quien crea la iglesia             | Todos, incluido `org.delete` de la iglesia y `iam.manage`                                                                  |
| **Administrador** | Pastor o secretaría               | Todos menos borrar la iglesia                                                                                              |
| **Líder**         | Líder de grupo, ministerio o sede | `org.read/update/create_child`, `members.*`, `events.*`, `attendance.write`, `invitations.send` en su nodo y descendientes |
| **Colaborador**   | Ujier, asistente                  | `org.read`, `members.read`, `events.read`, `attendance.write`                                                              |
| **Miembro**       | Cualquier miembro con cuenta      | `org.read` (información básica), `events.read` de lo que es visible para miembros                                          |

Reglas:

- Política efectiva = unión de los bindings en el nodo y en todos sus ancestros (el nodo y sus ancestros, con un CTE recursivo).
- Al crear una iglesia, el creador recibe **Propietario** en la iglesia.
- Quien figura como **líder** de un grupo o ministerio (la accountability `LEADER`) y tiene usuario vinculado obtiene los permisos del rol Líder en ese nodo (004b, sin binding: sale de la vista).
- Escalada de privilegios: dar, quitar, crear, borrar o cambiar el nivel de un cargo que da acceso exige `iam.manage` en el nodo y tener todos los permisos efectivos del cargo. Los cargos sin permisos solo piden `members.write`. Se impide quitar el último Propietario, y solo un Propietario agrega o quita Propietarios.
- Enforcement en la API: un `AuthorizationService.check(userId, nodeId, permission)` llamado desde cada Command/QueryHandler (o un `@PreAuthorize` con un bean). Los endpoints que reciben `eventId` o `personId` resuelven primero su nodo. La consulta de bindings se cachea por request.
- `GET /v1/me/permissions?organizationId=` devuelve los permisos efectivos. La UI solo esconde botones; la API sigue siendo la autoridad.

## 4. Flujo de la app

1. **Registro e ingreso**: Auth0 Universal Login con signup. Al entrar, `/portal` llama a `GET /v1/me` y a `GET /v1/me/organizations` (los nodos donde el usuario tiene un binding, más sus ancestros, para armar el árbol).
2. **Sin iglesias**: asistente "Crea tu iglesia" en 2 pasos (iglesia, luego primera sede, que es obligatoria), enviado a un endpoint **atómico** `POST /v1/churches { name, ..., location: {...} }`. Si en cambio tiene una invitación pendiente, ve "Tienes invitaciones" y puede aceptarlas.
3. **Con iglesias**: se redirige al último nodo usado (`localStorage`) o, si no hay, al primero.
4. **Selector de contexto** como el _resource picker_ de GCP, en la barra superior: breadcrumb `Iglesia › Sede › Ministerio › Subgrupo`. Un diálogo con buscador y árbol permite cambiar de iglesia o de cualquier nodo. Reemplaza `LocationsDialog`.
5. **Rutas**: `app/portal/[locId]` pasa a `app/portal/[nodeId]`. Las secciones (personas, eventos, subgrupos, accesos) se muestran según el tipo de nodo y los permisos.
6. **"Mi espacio"** (`/portal/me`) para cualquier usuario: mis cursos y eventos (donde estoy inscrito o asistí), mis grupos y ministerios, y el directorio de la iglesia (nodos visibles con su información básica y sus líderes).
7. Nueva pestaña **"Accesos"** en cada nodo (como "IAM" en GCP): bindings heredados, marcados como tales, y directos; otorgar o quitar roles; invitaciones pendientes.

## 5. Invitaciones

- Tabla `invitation`: `id`, `email`, `organization_id`, `role_id`, `person_id` (nullable), `token_hash`, `status` (PENDING/ACCEPTED/REVOKED/EXPIRED), `expires_at` (14 días), `invited_by`.
- `POST /v1/organizations/{id}/invitations` requiere `invitations.send` y solo puede otorgar roles que el invitador tiene. Envía un correo con el enlace `/{origen}/invitacion/{token}`.
- Para aceptar: si no hay sesión, login o signup. Luego `POST /v1/invitations/{token}/accept`, que exige que el email **verificado** del token sea igual al de la invitación. Crea el binding y vincula `app_user.person_id` con la persona.
- Desde el formulario de grupo o ministerio, al elegir líderes que tienen email: casilla "Invitar a usar la app", marcada por defecto. Requiere agregar `email` a `person`, que hoy no lo tiene.
- Proveedor de correo: **Resend**. Un `EmailGateway` en el módulo `notification` usa el `RestClient` de Spring (sin agregar un SDK nuevo, según la regla de dependencias mínimas). La API key va en Secret Manager (`sm@resend-api-key`). En `local` y en las pruebas se usa un gateway que solo registra el correo en el log.

## 6. Visibilidad de sedes, grupos y ministerios

Campo `organization.member_visibility`:

- `PUBLIC` (**por defecto**): cualquiera con el enlace, sin sesión, ve la página pública del nodo.
- `CHURCH`: cualquier usuario con un binding en la iglesia.
- `MEMBERS`: solo los miembros de ese nodo y quienes tengan `members.read` heredado.
- `LEADERS`: solo quienes tengan `members.read`.

La **información básica** del nodo (nombre, descripción, horario, líderes) se ve siempre en el directorio de la iglesia. `member_visibility` controla la **lista de miembros**.

Qué muestra la vista pública (decidido con el usuario):

- **Grupo o ministerio**: información básica, **líderes destacados** y luego los miembros. De todos solo nombre y apellido, **sin edad, fecha de nacimiento, teléfono ni dirección**. Nunca lista a menores de 18 (protección de datos según la constitución del API).
- **Iglesia o sede**: además, un **organigrama**: el árbol de sedes, ministerios y grupos con sus líderes, y los cargos ministeriales (pastor, etc.). Lo controla `organization.show_org_chart`, **activado por defecto**. Solo incluye los nodos hijos que no sean `LEADERS`. Página pública: `GET /v1/public/organizations/{slug}` (sin JWT, con slug no adivinable) y la ruta UI `/g/{slug}`.

## 7. Eventos y cursos: compartir, inscribirse y asistir

Son tres cosas distintas, cada una con su propio mecanismo (`specs/007-sharing`, `specs/011-registration`).

### Compartir (quién puede abrir el evento)

- Por defecto heredan el acceso del nodo al que pertenecen.
- **Acceso general** (`event.general_access`): `RESTRICTED` (solo quien tiene acceso por la jerarquía o compartido directo), `CHURCH` (cualquier usuario de la iglesia) o `ANYONE_WITH_LINK`. El enlace solo deja **ver**: ya no hay «rol del enlace» (se quitó `event.link_role` y `REGISTRANT`).
- **Compartido directo**: tabla `event_share` (`event_id`, `email`, `role` VIEWER/EDITOR). Aplica al correo verificado, tenga cuenta ya o no.
- Enlace: `share_slug` aleatorio, regenerable para revocar. Ruta pública `/e/{slug}` y `GET /v1/public/events/{slug}`.
- UI: diálogo «Compartir» con un único botón **Guardar** (nunca guarda solo): personas con acceso, acceso general y «Copiar enlace».

### Inscripción (quién tiene un lugar)

- Es **opcional**: un evento nace sin inscripción (un culto no la usa). Se activa con una configuración muy corta: **cupos** (la cantidad del ticket; vacío es sin límite) y **precio** informativo. En «Más opciones»: exigir cuenta y varios **tipos de ticket** (nombre, precio, cantidad, ventana de venta). **El precio es informativo: no se cobra** (deuda técnica). **Cerrar y reabrir** las inscripciones es una acción aparte (`PUT /events/{id}/registration/open`), no un campo.
- **Un permiso por acción**: `registrations.read` (ver configuración e inscritos), `registrations.configure` (activar, cambiar, quitar), `registrations.close` (cerrar y reabrir) y `registrations.remove` (quitar a un inscrito); marcar el ingreso es `attendance.write`. Quien tenía `events.write` recibió los cuatro nuevos al migrar.
- No hay lista de espera: al llenarse dice «Agotado» y, si alguien cancela, queda libre para quien llegue. (Se quitó del todo en la migración `V1.14.0` porque nadie avisaba a quien esperaba; si vuelve, irá con los avisos por correo de §11.)
- Tablas `event_registration_setup`, `ticket_type` y `event_registration` (estado `CONFIRMED` o `CANCELLED`). Los cupos se cuentan bajo un bloqueo de la configuración.
- Cada inscripción tiene un `cancel_code` (para cancelar sin cuenta) y un `ticket_code` (el QR del ticket; no sirve para cancelar).
- Quien puede abrir la página pública se inscribe en `POST /v1/public/events/{slug}/registrations`.
- Mi espacio: **«Mis eventos y cursos»** = lo que tengo con lugar (con botón para cancelar) y lo que asistí; **«Compartidos contigo»** = lo que otra persona o la iglesia me dejó ver y todavía no es mío por inscripción ni asistencia. Cada tarjeta dice por qué aparece.

### Asistencia (quién estuvo)

- Es independiente de la inscripción. Un culto marca personas de la iglesia, como siempre (`ATTENDANCE_WRITE`).
- **Visitantes**: quien llega por primera vez es una persona con estado `VISITOR` (luego `MEMBERSHIP_IN_PROGRESS` y `MEMBER`; son las relaciones de estado de siempre y un cargo implica `MEMBER`). Quien no quiere dar su nombre, o a quien el ujier no alcanzó a anotar, se suma al contador `event_attendance.guest_count`.
- **QR del evento**: el ujier lo muestra; la gente lo escanea y queda marcada (con cuenta, la persona vinculada y su ticket; sin cuenta, un visitante si da su nombre o un conteo si no). El código se firma con HMAC, cambia cada 5 minutos y solo vale alrededor del evento.
- **QR del ticket**: el ujier lo escanea y la inscripción pasa a ingresada (una sola vez); si la cuenta es una persona de la iglesia, también queda en la asistencia.
- Cada marca guarda su origen (`MANUAL`, `QR_EVENT`, `QR_TICKET`) y quién la hizo.

## 8. Migración de datos

El usuario confirmó que **no hay datos reales**, así que hay libertad para limpiar:

- Nueva migración `V1.1.0__identity_iam.sql`:
  - Borra todas las filas de dominio: `LCDP`, organizaciones, personas, eventos y accountabilities de prueba.
  - Borra los cargos globales.
  - Agrega `parent_id`, `path`, los enlaces sociales, la visibilidad y las tablas nuevas.
  - No se reescribe `V1.0.0`, para no romper el checksum de Flyway en las BD de dev y prod ya migradas.
- **Sin migración perezosa**: se elimina el claim `orgId`, `Auth0Gateway`, `update_user_app_metadata`, `Auth0Config` y su configuración de client credentials.

## 9. Entregas (cada una es una feature spec-kit en `specs/NNN-*`, PR en ambos repos)

1. **003-identity-iam**:
   - API: `app_user`, `/me`, `parent_id`/`path`, IAM con roles predefinidos y enforcement, y cargos por iglesia con sugerencias.
   - API: `POST /churches` atómico (iglesia, primera sede y cargos sugeridos, con enlaces sociales opcionales).
   - API: limpieza de datos y retiro de `Auth0Gateway`.
   - UI: onboarding (asistente con cargos sugeridos y "Otras opciones"), `/portal/[nodeId]`, selector de contexto, permisos en la UI y autocompletado de cargos.
2. **004-access-management**: pestaña "Accesos" (bindings heredados y directos, otorgar y quitar), reglas de escalada de privilegios y **roles IAM personalizados**. Los grupos y ministerios anidados ya se hicieron en la 003.
3. **004b-positions-with-access**: el cargo lleva el nivel de acceso; se quita "otorgar rol suelto" (queda solo Propietario); el alta de iglesia pide un nivel por cargo y el cargo de quien crea, que queda como persona vinculada a su usuario; el alta de iglesia pide los permisos de cada cargo (con valores sugeridos) y el cargo de quien crea, que queda como persona vinculada a su usuario; "Cargos" y "Accesos" se unen en una pantalla «Cargos y accesos» con pestañas. Va antes de la 005 para que la invitación solo pida correo y cargo.
4. **005-invitations**: `person.email`, invitaciones (correo + cargo), aceptación (vincula usuario y persona), "invitar líderes".
5. **006-visibility**: `member_visibility`, directorio, páginas públicas de nodos.
6. **007-sharing**: compartir eventos y cursos, enlaces, inscripción por enlace.
7. **008-my-space**: la vista del miembro común y el nivel Miembro (cuenta sin cargo).
8. **010-recurring-events-calendar-sync**: eventos recurrentes estilo Google Calendar (cada N, días, fin nunca/fecha/cantidad) y calendario privado `.ics` para suscribirse desde Google Calendar o Apple Calendar.
9. **011-registration**: inscripción como proceso aparte (tickets y cupos), asistencia con QR del evento y del ticket, visitantes y rediseño de Mi espacio. Reemplaza el «rol del enlace» de la 007.
10. **009-tech-debt**: no es una feature, es una lista viva de limpiezas y pendientes (sección 10). Cada ítem se hace solo con el visto bueno del dueño del producto.

Alcance acordado para este branch (`claude/app-flow-redesign-nq6j60`, en ambos repos):

1. Guardar este diseño como `docs/redesign.md` en los dos repos, como referencia para las fases siguientes.
2. Correr `speckit-specify`, luego `speckit-plan` y luego `speckit-tasks` para la **003**, primero en el API y después en la UI.
3. Implementar la 003: primero el API, luego `pnpm api:sync` y la UI.
4. Commits en Conventional Commits y push al branch. No se abre PR salvo que el usuario lo pida.

Sin la Auth0 Action que agrega el email al token, el usuario se registra sin email (ver el ítem 6 de la sección 10).

## 10. Deuda técnica y limpieza pendiente (009)

**Regla:** cada ítem se consulta con el dueño del producto antes de hacerlo; nada de aquí se borra ni se cambia por iniciativa propia. Todo lo que quede por hacer o por limpiar al cerrar una fase se agrega a esta tabla.

| #   | Ítem                                      | Repo     | Por qué quedó                                                                                                                                                                                    | Qué haría falta                                                                                                                                            | Estado                |
| --- | ----------------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- |
| 1   | Secretos `auth0-client-*`                 | API      | Desde la 003 la API no llama a la Management API de Auth0, pero `infra/terraform/environments/{dev,prod}/main.tf` aún listan `existing_secret_ids = ["auth0-client-id", "auth0-client-secret"]`. | Quitarlos de Terraform y aplicar; borrar los secretos en Secret Manager; revisar o borrar la aplicación M2M de Auth0 con permisos sobre la Management API. | Pendiente (consultar) |
| 2   | Claim y metadata `orgId` en Auth0         | Auth0    | La UI ya no lee `https://sr-tommy.com/orgId`, y la API ya no escribe `app_metadata.orgId`.                                                                                                       | Revisar en el tenant si hay una Action o Rule que agrega el claim y quitarla; opcionalmente limpiar el `app_metadata.orgId` de los usuarios.               | Pendiente (consultar) |
| 3   | Nombres `/organizations/locations/childs` | API y UI | Ahora crean y editan grupos a cualquier profundidad, no solo hijos de una sede.                                                                                                                  | Renombrar (p. ej. `/organizations/{id}/groups`); rompe el contrato: `pnpm api:sync` y ajustar la UI y las e2e.                                             | Pendiente (consultar) |
| 4   | Carpeta `app/portal/[nodeId]/location`    | UI       | Ahora muestra los datos de cualquier nodo (iglesia, sede o grupo).                                                                                                                               | Renombrar a `details` y actualizar enlaces y e2e.                                                                                                          | Pendiente (consultar) |
| 5   | Columna `app_user.person_id`              | API      | Se reservó para la 005, pero la 004b la reemplazó por la tabla `user_person` (un usuario puede estar en varias iglesias) y la migración `V1.3.0` la borró.                                       | Nada que hacer; quitar este ítem de la lista si el dueño está de acuerdo.                                                                                  | Resuelta (004b)       |
| 6   | Email del usuario sin Auth0 Action        | API      | El diseño mencionaba leer el email desde `/userinfo` si el token no lo trae; no se implementó.                                                                                                   | Crear la Auth0 Action del README del API (recomendado) o implementar el respaldo con `/userinfo`.                                                          | Pendiente (consultar) |
| 7   | Binding Líder al asignar un líder         | API      | La 004b lo resolvió sin binding: la vista `user_access` da el nivel Líder a quien figura como líder y tiene usuario vinculado.                                                                   | Nada que hacer; quitar este ítem de la lista si el dueño está de acuerdo.                                                                                  | Resuelta (004b)       |
| 8   | Bindings de roles personalizados borrados | API      | Desde la 004b `role_binding` solo guarda Propietarios (nunca un nivel propio) y un nivel en uso no se puede borrar, así que ya no quedan bindings de niveles borrados.                           | Nada que hacer; quitar este ítem de la lista si el dueño está de acuerdo.                                                                                  | Resuelta (004b)       |
| 9   | Nivel "Miembro" sin cargo                | API y UI | La 004b solo daba acceso por cargo; una persona con cuenta y sin cargo no tenía ningún acceso.                                                                                                  | Nada que hacer: la 008 agregó la rama `MEMBER` a `user_access` (V1.7.0) y la UI la ruta `/portal/me`. Quitar este ítem de la lista si el dueño está de acuerdo. | Resuelta (008)        |
| 10  | `NamesInput` sin uso                                 | UI       | El alta de iglesia dejó de usarlo: ahora usa `PositionLevelsField` (cargo + nivel). Quedan el componente y su prueba.                                                                            | Borrar `app/(components)/NamesInput.tsx` y su prueba.                                                                                                      | Pendiente (consultar) |
| 11  | `reachLabel` y el tipo de nodo en `permissionLabels` | UI       | «Personas con acceso» ya no explica lo que permite un rol según el nodo; solo lo usaban esa pantalla y su vista previa.                                                                                                         | Quitar `reachLabel`, `nodePermissionLabels` y el parámetro de tipo de nodo, o reutilizarlos para explicar un cargo.                                        | Pendiente (consultar) |
| 12  | Pantalla de roles propios (`positions/levels`)       | UI       | La UI de cargos solo muestra permisos; los roles personalizados no se ven, pero `RolesManager`, `RoleDialog` y la ruta `positions/levels` siguen ahí, sin enlace. Se dejan para cuando el sistema crezca y haya roles en la UI. | Decidir si se borran hasta entonces o se conservan.                                                                                                        | Reservada (consultar) |
| 13  | Remitente del correo (Resend)                        | API      | El secreto `resend-api-key` ya lo crea Terraform, pero sin un dominio verificado Resend solo entrega a la cuenta dueña. | Verificar un dominio en Resend y fijar `resend.from`. | Pendiente (consultar) |
| 14  | Variable `APP_URL` en prod                           | API      | El deploy de dev ya define `APP_URL=https://srtommy-ui-test.vercel.app`; el de prod no, y por defecto es `http://localhost:3000`. | Definir `APP_URL` en `deploy-prod.yml` con el origen de la UI de prod. | Pendiente (consultar) |
| 15  | Correo de los líderes ya guardados                   | API y UI | La lista de miembros de un grupo no trae el correo; al editar un grupo solo se invita a los líderes elegidos en esa sesión.                              | Agregar `email` a la respuesta de miembros del grupo y ofrecer «Invitar» también a los líderes guardados.                                                    | Pendiente (consultar) |
| 17  | Inscritos de un curso que no son personas            | API y UI | La inscripción por enlace guarda nombre, correo y teléfono, pero no crea la persona de la iglesia ni toma asistencia con ellos.                                          | Un botón «Agregar a la iglesia» que cree la persona desde la inscripción y la una al nodo del curso. (011: quien ingresa con su ticket ya cuenta como asistente aunque no sea persona; los visitantes que se anotan en la puerta sí son personas con estado `VISITOR`.)                                                         | Pendiente (consultar) |
| 18  | Compartir una ocurrencia de una serie                | API y UI | Cada ocurrencia de una serie es un evento con identidad propia (desde la 010), pero el compartido todavía se hace evento por evento: no se comparte la serie completa de una vez. | Decidir si compartir una ocurrencia debe poder extenderse a toda la serie.                                                                                   | Pendiente (consultar) |
| 19  | Extender series sin fin en Cloud Run                 | API      | Era `ExtendEventSeriesJob` con `@Scheduled` dentro de la app; Cloud Run puede escalar a cero y no correrlo. | Hecho: `POST /v1/internal/events/extend-series`, llamado a diario (03:30 UTC) por Cloud Scheduler (`infra/terraform/modules/scheduler`). | Resuelta |
| 20  | Zona horaria de la organización                      | API y UI | La zona de una serie se toma del navegador de quien la crea; no existe una zona por iglesia.                                                                              | Guardar la zona en la organización y usarla como valor por defecto al crear eventos.                                                                         | Pendiente (consultar) |
| 21  | Política de reintentos y límites de la cola `outbox-email` | API (Terraform) | `infra/terraform/modules/tasks` creó la cola con valores por defecto sin medirlos: 10 intentos, espera de 60 s a 3600 s (5 duplicaciones), 12 h de duración máxima, 10 envíos por segundo y 5 concurrentes. Diez intentos parecen demasiados para un correo (los errores permanentes de Resend ya fallan de inmediato), y 10 por segundo probablemente supera el límite de Resend (del orden de 2 por segundo; confirmar en su documentación), lo que daría 429 y reintentos inútiles. | Revisar con datos reales de dev: tasa de fallos, cuánto duran las caídas de Resend y cuántos 429 hay. Candidato inicial: 5 intentos en cerca de una hora, `max_dispatches_per_second` por debajo del límite de Resend, y `max_retry_duration` bien por debajo de 24 h (Resend recuerda la clave de idempotencia 24 h). Cambiar `max_attempts` en el módulo `tasks` y `outbox.max-attempts` en `application-prod.yml` a la vez. | Pendiente (consultar) |
| 22 | Pagos reales en la inscripción | API y UI | La 011 guarda el precio del ticket solo como dato: no se cobra ni hay pasarela. | Elegir pasarela (Stripe o Mercado Pago), estado de pago en la inscripción, webhooks y reembolsos. El modelo ya tiene el precio por ticket. | Pendiente (consultar) |
| 23 | Correos de la inscripción | API | El código para cancelar solo se muestra al inscribirse y en Mi espacio; no se envía un correo de confirmación ni se avisa a quien pasa de la espera a confirmado. | Encolar el correo por el outbox (`SendEmailCommandHandler`) al inscribirse y al promover desde la espera, con el enlace de cancelación. | Pendiente (consultar) |
| 24 | Límite de peticiones en los endpoints públicos | API (infra) | La inscripción y el QR del evento aceptan peticiones sin cuenta: alguien podría crear muchas inscripciones o muchos visitantes. | Límite por IP en Cloud Run o Cloud Armor, y revisar si el QR sin cuenta debería pedir un captcha. | Pendiente (consultar) |
| 25 | Aplicar Terraform: `attendance-qr-secret` | API (infra) | El módulo `db-secrets` ahora genera el secreto que firma el QR de asistencia. Hasta aplicarlo, en dev y prod el QR responde error (el secreto está vacío) pero el resto de la API funciona. No se pudo validar con `terraform` desde el entorno de desarrollo. | Correr `terraform apply` en dev y en prod antes de usar el QR; rotar el secreto invalida los QR abiertos. | Pendiente (consultar) |
| 26 | Guardar la lista de asistencia puede quitar marcas hechas por QR | API y UI | `PUT /events/{id}/attendance` reemplaza la lista completa: si alguien se marca por QR mientras el ujier tiene la pantalla abierta, su marca se pierde al guardar. | Que la UI vuelva a leer antes de guardar (o refresque sola), o un endpoint que solo agregue y quite diferencias. | Pendiente (consultar) |
| 27 | Visitantes sin diferenciar en el nivel «Miembro» | API | Toda persona vinculada a un usuario obtiene «Miembro» en su nodo (008) aunque sea visitante, porque hoy todas nacen visitantes. | Decidir si un visitante vinculado debe tener menos acceso y, de ser así, excluirlo de la rama `MEMBER` de `user_access` y promover a quienes ya están. | Pendiente (consultar) |
| 28 | Preguntas propias y varios boletos por compra | API y UI | La inscripción pide solo nombre, apellido, correo y teléfono, y un boleto por inscripción. | Preguntas configurables por evento y cantidad de boletos por compra. | Pendiente (consultar) |
| 29 | Historial al quitar la inscripción de un evento | API | Quitar la inscripción borra también las filas canceladas (solo guardaban historial). | Decidir si ese historial debe conservarse (por ejemplo, desactivar en vez de borrar). | Pendiente (consultar) |
| 30 | Consultar el historial de membresías | API y UI | Quitar a una persona de un nodo o de la iglesia cierra sus `accountability` (`deleted_time`) en vez de borrarlas, y la persona queda con `person.deleted_time`. Los datos se conservan, pero ninguna pantalla ni endpoint responde «quién estaba en X en la fecha Y». | Endpoint y vista de historial por nodo/persona leyendo las `accountability` cerradas (hoy `deleted_time` hace de fecha de cierre; la de inicio es `create_date`). | Pendiente (consultar) |
| 31 | `Accountability` vuelve a tener soft delete | API | El `TODO: remove soft delete` de `Accountability` pedía quitar `deleted_time`; la eliminación de personas (specs: eliminar persona) lo necesita para conservar el historial, así que se reactivó `@SQLRestriction("deleted_time is null")`. | Confirmar con producto que este es el modelo definitivo; si lo es, cambiar `deleted_time` por un nombre que diga «cerrada» y retirar el TODO de la entidad. | Pendiente (consultar) |
| 32 | Invitaciones y expositores de personas eliminadas | API | Al eliminar una persona de la iglesia no se revocan sus invitaciones pendientes, y si es expositora de un evento sigue apareciendo con su nombre. | Revocar las invitaciones `PENDING` de la persona al eliminarla y decidir cómo mostrar a un expositor eliminado. | Pendiente (consultar) |
| 33 | Eliminarse a uno mismo | API | Quien tiene `members.delete` puede eliminar a la persona vinculada a su propia cuenta; pierde los cargos que le daban acceso (el rol Propietario directo se conserva). | Rechazar con un mensaje claro cuando la persona a eliminar es la que representa al usuario que pide, o exigir otro administrador. | Pendiente (consultar) |
| 34 | Pool de conexiones frente al límite de Neon | API (infra) | `hikari.maximumPoolSize` está en 2 (`application-prod.yml`) y el deploy no fija `--max-instances`, así que Cloud Run puede escalar hasta su máximo por defecto. El plan gratis de Neon da un compute de 0.25 CU con `max_connections` de unas 104 a 112 (~97 usables según su documentación; confirmar en la consola) y la URL usa el host directo, sin `-pooler`: cada conexión de Hikari es un backend de Postgres. Con virtual threads, 2 conexiones por instancia hacen cola, pero subir el pool sin acotar las instancias podría superar el límite de la base. | Fijar `--max-instances` en `deploy-dev.yml` y `deploy-prod.yml` (por ejemplo 5) y subir `maximumPoolSize` a 5–10 cuidando `pool × instancias ≤ ~60`; poner `minimumIdle` en 0 o 1 para que Neon pueda suspenderse tras 5 min sin actividad. Antes, revisar el máximo actual con `gcloud run services describe` (puede estar fijado fuera del repo) y el tamaño real del compute en la consola de Neon. No usar el endpoint `-pooler` sin mover las migraciones de Flyway al host directo: los locks de sesión no funcionan en modo transacción. | Pendiente (consultar) |
| 35 | Eventos compuestos al editar fechas | API | La edición (013) deja un punto de extensión (`validateSchedule` en `EditEventCommandHandlerImpl`) sin implementar: cuando la spec 012 (mega eventos con sub-eventos) llegue a `main`, al cambiar las fechas de un mega evento sus sub-eventos deben seguir dentro, y las de un sub-evento dentro de su mega evento (hoy solo se valida al crear). | Implementarlo en `validateSchedule` cuando exista `Event.parent`. | Pendiente (consultar) |
| 36 | Avisar de más cambios de un evento | API | Al editar avisa por correo a los inscritos cuando cambia la fecha, la hora, el nombre, el lugar o los ponentes. No avisa a quien tiene el evento compartido sin inscribirse, ni por WhatsApp, ni por un cambio de descripción. | Decidir con producto qué cambios avisan y a quién; hacerlo por el outbox. | Pendiente (consultar) |
| 37 | Reunir ocurrencias sueltas a una serie | API y UI | Al cambiar la regla de una serie, las ocurrencias con inscritos, asistencia o ya pasadas quedan como eventos sueltos intactos (013, decisión 3); no hay forma de volver a unirlas a la serie nueva ni de verlas como grupo. | Definir si hace falta (p. ej. «unir a la serie» o un aviso en el detalle). | Pendiente (consultar) |

## 11. Trabajo futuro: centro de notificaciones (campana)

No se construye en 005-009; queda documentado para no perder la idea. Una campana en la cabecera de la app, **genérica**: no pensada solo para invitaciones, sino para cualquier aviso dirigido a un usuario.

- **Modelo**: tabla `app_notification` (`id`, `user_id`, `type`, `payload` JSON, `created_at`, `read_at`), con tipo y datos mínimos para armar el texto y el enlace en la UI. Cada aviso que salga de la app (correo, WhatsApp) se entrega por el outbox (§12); la campana es un canal más, que escribe en `app_notification`.
- **API**: `GET /v1/me/notifications` (paginado, con contador de no leídas), `POST /v1/me/notifications/{id}/read` y `POST /v1/me/notifications/read-all`. La UI consulta el contador con SWR (polling suave) y abre un menú con la lista.
- **Tipos iniciales**:
  - Invitación aceptada: avisa a quien invitó ("Ana aceptó tu invitación a la sede X").
  - Invitación recibida: avisa al invitado que ya tiene cuenta ("Te invitaron a unirte a la iglesia XYZ"), con acceso directo a aceptar. Sustituye al aviso solo por correo y alimenta «Mi espacio» (008).
  - Cumpleaños cercanos: se calculan desde `person.birthdate` con un job diario por nodo, solo para quienes tienen permiso de ver a las personas (y sin exponer menores a quien no deba verlos, según la regla de §6). Cada usuario puede silenciarlos.
  - Avisos del sistema: mantenimiento, novedades y cambios de términos, enviados por el equipo a todos o a un grupo de usuarios.
  - Otros candidatos: evento compartido contigo (007), inscripción nueva a un curso, cargo asignado o retirado.
- **Preferencias**: pantalla para elegir qué tipos recibir y por qué canal; los avisos del sistema no se pueden silenciar.
- **Decisiones abiertas**: retención (p. ej. 90 días), si el cumpleaños se genera al vuelo (sin guardar filas) y si hace falta tiempo real (SSE) o basta el polling.

## 12. Trabajo futuro: mensajería multicanal y agentes en WhatsApp

La entrega de mensajes ya está resuelta para correo (módulo `outbox`, ver «Trabajo programado y sistemas externos» en `docs/code-style.md`). Lo que sigue se apoya en el mismo contrato (la tabla `outbox_message` y un `OutboxMessageHandler` por canal), sin cambiar a quien encola.

- **Envíos programados**: cumpleaños (desde `person.birthdate`) e invitaciones a cursos y eventos. Un job diario de Cloud Scheduler recorre los nodos, inserta los mensajes con `dispatchNow = false` y programa las tareas en lotes; el barrido diario recoge las que falten.
- **Canal WhatsApp** (`WhatsAppOutboxHandler`, cola `outbox-whatsapp` con su propio límite de velocidad por los límites de Meta): plantillas aprobadas para escribir primero, ventana de 24 horas para responder libremente y **consentimiento previo** del destinatario (un dato nuevo en la persona o el usuario). El estado de la fila ganaría `DELIVERED` y `READ`, y `provider_message_id` casa los avisos del proveedor.
- **Mensajes entrantes (agentes)**: cada webhook se guarda primero en un **inbox** (tabla con el id del mensaje del proveedor como clave única, para que un reintento del webhook no ejecute dos veces la misma acción) y se procesa con una tarea de Cloud Tasks. El agente ejecuta los mismos comandos del API con la identidad del usuario vinculado a ese número, y responde por el outbox.
- **Si crece el volumen o aparecen otros servicios** (un MCP en Python, por ejemplo): leer la tabla con Debezium o Datastream y publicar en Pub/Sub, sin tocar a quien encola.

## Archivos clave

- API: `src/main/resources/db/migration/` (nueva `V1.1.0__…`), `J/shared/SecurityConfig.java`, `J/organization/command/OrganizationWriter.java`, `J/organization/command/create_church/CreateChurchCommandHandlerImpl.java`, `J/auth/gateway/Auth0Gateway.java` (se elimina el uso de app_metadata), `J/organization/controller/OrganizationController.java`. Módulos nuevos: `user/`, `iam/`, `invitation/`, `notification/`, siguiendo `docs/code-style.md` y `ArchitectureTests`.
- UI: `app/portal/page.tsx` (onboarding), `app/portal/MenuDrawer.tsx` y `LocationsDialog.tsx` (pasan a ser el selector de contexto), `app/portal/[locId]/**` (pasa a `[nodeId]`), `app/portal/location/create/page.tsx`, `app/(api)/openapi/srtomy-api.json` (con `pnpm api:sync`).

## Verificación

- API: `./mvnw verify`, con pruebas de integración de la herencia de permisos (un binding en la iglesia alcanza a un subgrupo nieto; uno en un grupo no alcanza a su sede), de la escalada de privilegios, del último Propietario, de la aceptación de invitaciones con email distinto (rechazada) y de la vista pública sin datos de menores.
- UI: `pnpm check:fast`, pruebas unitarias del selector y del onboarding con user-event. Playwright: signup → crear iglesia y sede → cambiar de contexto; un miembro no ve "Accesos"; un enlace público de evento abre sin sesión.
