# Feature Specification: Estado por nodo, personas en grupos y crecimiento

**Feature Branch**: `claude/person-registration-flow-redesign-cyonkh`

**Created**: 2026-10-29

**Status**: Implemented (API y UI, esta última en el repo `srtomy-ui`)

**Input**: El dueño del producto pidió rediseñar el flujo de registrar personas: «Personas» y «Miembros» (dentro del formulario del grupo) eran la misma cosa en dos pantallas, no había forma de registrar a alguien directamente en un grupo y no se podía ver si la iglesia crece. Diseño de pantallas: ver el repo `srtomy-ui`.

## Decisiones (acordadas con el dueño del producto)

1. **Una persona existe una sola vez en la iglesia.** Lo que cambia es a qué nodos pertenece (sede, grupo, ministerio) y cómo está en cada uno. «Registrar» y «agregar a un grupo» son el mismo acto: buscar primero y, si no existe, crearla.
2. **El estado (visitante, asistente, miembro) es por nodo.** Ya era así en el modelo (`accountability` de categoría `MEMBER_PROGRESS_STATE` entre persona y nodo). Se puede ser asistente del grupo de jóvenes y miembro de la iglesia. No hay estado «global».
3. **Quien está en un grupo está en su iglesia.** Al poner a alguien en un grupo (o al registrarlo en una sede o grupo) la iglesia y los nodos intermedios que todavía no lo conocen lo reciben con el mismo estado. Es solo el **estado** (`accountability` de progreso), no `IS_PART_OF`: `IS_PART_OF` da el nivel Miembro (008) en ese nodo y en todo lo que cuelga, y propagarlo habría dado acceso a la iglesia entera. Quien ya tiene un estado en la iglesia lo conserva: se puede ser asistente del grupo de jóvenes y miembro de la iglesia. Cada nodo tiene su propio estado y la lista de un nodo muestra el de ese nodo.
4. **El `LEADER` antiguo ya no existe.** Liderar se expresa con **cargos**, que ya son por nodo y pueden ser varios por grupo (líder, tesorera...). Al crear una iglesia hay **un solo** cargo sugerido para eso: «Líder de grupo o ministerio». `V1.17.0` quita la rama `LEADER` de `user_access`, borra las relaciones `LEADER` y su tipo. Un cargo ya da sus permisos en el nodo y debajo.
5. **El formulario del grupo ya no reemplaza a su gente.** `PUT /organizations/locations/childs/{id}` solo edita los datos del grupo; `POST` acepta `memberIds` (entran como miembros) para crear un grupo ya poblado, pero la UI no lo usa. Se quitó `leaderIds` y el endpoint `GET /organizations/locations/childs/{id}/members`, que nadie usaba.
6. **Quien tiene un cargo en un nodo es miembro de ese nodo**: darle otro estado ahí responde 400 (misma regla que ya existía al editar la persona).
7. **Historial de estados.** Cada alta en un nodo, cambio de estado y salida queda en `member_state_event` (`from_state` nulo = entró, `to_state` nulo = salió). Es historia: no se edita ni se borra. Lo escriben `CreateAccountabilities` (alta con estado), `SetMemberState` (cambio) y `EndAccountabilitiesOfChild` (salida), así que ningún camino de entrada se salta el registro. `V1.16.0` crea la tabla y la rellena con quienes ya estaban.
8. **Se quitó el tablero de estados** (`GET /organizations/{id}/member-state`): la lista de personas ya muestra y filtra por estado.
9. **Crecimiento** (`GET /organizations/{id}/growth?months=12`, de 1 a 36): cuántas personas hay hoy por estado y, mes a mes, cuántas **entraron**, **pasaron a asistente**, **pasaron a miembro** y **salieron**, más cuántas personas y miembros había al terminar cada mes. Se calcula reproduciendo los eventos del nodo y de todo lo que cuelga de él: una persona cuenta una vez, con su estado más alto; entrar a un segundo nodo no es «entrar» y salir de uno de varios no es «salir». Pide `members.read` en el nodo: lo ven quienes tienen cargo con ese permiso (líderes del grupo, de la sede o de la iglesia), cada uno sobre lo suyo.

## Endpoints

- `PUT /people/{id}/memberships/{organizationId}` `{memberState}` — pone a una persona que ya está en la iglesia en el nodo (y en la iglesia, si no la conocía), o cambia su estado en el nodo. Pide `members.write` en el nodo y que la persona ya pertenezca a la misma iglesia (si no, 403). 204.
- `GET /people/{id}/memberships` — nodos de la persona con su estado y sus cargos; solo los nodos donde quien pide tiene `members.read`.
- `GET /people?organizationId=&memberState=&searchText=` — cada persona trae `memberState` (su estado en ese nodo; vacío si solo está en un nodo de abajo). `memberState` filtra por ese estado.
- `GET /organizations/{id}/growth` — ver 8.

Quitar a alguien de un nodo sigue siendo `DELETE /people/{id}?organizationId=` (cierra sus relaciones del nodo y de lo que cuelga, y deja el evento de salida).

## User Scenarios & Testing

1. **Given** Lucía, miembro de la sede, **When** se la agrega al grupo Jóvenes como asistente, **Then** en Jóvenes es asistente, en la sede sigue siendo miembro y la iglesia la conoce sin darle acceso de miembro a la iglesia.
2. **Given** dos personas en Jóvenes, una visitante y otra asistente, **When** se lista Jóvenes con `memberState=MEMBERSHIP_IN_PROGRESS`, **Then** sale solo la asistente.
3. **Given** una persona que cambia de visitante a asistente y a miembro, **Then** `member_state_event` tiene tres filas en orden y el crecimiento del mes cuenta una entrada, un paso a asistente y un paso a miembro.
4. **Given** alguien con un cargo en la sede, **When** se le pone `VISITOR` ahí, **Then** 400.
5. **Given** una persona de otra iglesia, **When** se la quiere agregar a un grupo, **Then** 403.
6. **Given** un grupo con gente, **When** se edita sus datos, **Then** la gente se conserva.
7. **Given** una persona que sale de un grupo, **Then** queda un evento de salida y el crecimiento del grupo cuenta una salida; sigue en la sede.

## Directorio y «Mis grupos» sin `LEADER`

- **Mis grupos** (`GET /me/nodes`) son los nodos donde la persona está anotada (`IS_PART_OF`) o tiene un cargo; ya no trae `leader`. Quien solo tiene un estado (la iglesia que lo conoce por un grupo) no aparece.
- **Directorio y página pública**: «líderes» pasa a ser `team` (equipo): las personas que tienen un cargo en el nodo, una vez cada una. La lista de miembros de un nodo no repite al equipo.
