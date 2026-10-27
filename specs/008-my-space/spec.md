# Feature Specification: Mi espacio y el nivel Miembro

**Feature Branch**: `claude/008-my-space`

**Created**: 2026-10-25

**Status**: Implemented (API y UI, esta última en el repo `ovejafiel-ui`)

**Input**: Desde la 004b el acceso nace de los cargos: una persona con cuenta y **sin cargo** no tiene ningún acceso (deuda técnica 9). Esta fase le da el nivel **Miembro** y una vista propia, «Mi espacio» (`/portal/me`). Diseño: [`docs/redesign.md`](../../docs/redesign.md) §4.6 y §9.

## Decisiones

1. **Nivel Miembro implícito**: toda persona vinculada a un usuario (`user_person`) obtiene el rol predefinido **Miembro** (`org.read`, `events.read`; id `0195f0a0-0000-7000-8000-000000000005`) en cada nodo donde es miembro (`IS_PART_OF`) y en todo lo que cuelga de él. Es una cuarta rama (`MEMBER`) de la vista `user_access` (`V1.7.0__member_access.sql`); no hay filas nuevas en ninguna tabla.
2. **Es un piso, no un tope**: la vista da la unión de todas las ramas, así que un cargo, el liderazgo o Propietario siguen dando lo suyo; Miembro solo agrega lo mínimo (regla anti-escalada intacta: nadie obtiene más de lo que ya da su cargo, y nadie queda con menos que Miembro).
3. **No es un acceso concedido**: la lista «Personas con acceso» de un nodo (`GET /organizations/{id}/access`) **no** muestra la rama `MEMBER`; nadie la otorgó ni se puede quitar salvo dejando de ser miembro.
4. **Coherencia con la 006**: `ChurchAccess.requireMember` ya considera miembro de la iglesia a quien tiene algún nodo en `GET /me/organizations`; como ahora ser miembro de un nodo lo pone en ese árbol, un miembro sin cargo ve el directorio de su iglesia y Mi espacio, y quien no es miembro de nada sigue recibiendo 403.
5. **Menores**: el nivel Miembro solo da información básica; los datos de menores siguen sin salir en listas (`AgeRules` en el directorio) y los endpoints de Mi espacio devuelven **solo datos propios** (nodos y eventos de la persona vinculada al usuario), nunca de otras personas.
6. **Mi espacio reutiliza lo existente**: «compartidos conmigo» es `GET /me/shared-events` y las invitaciones pendientes son `GET /me/invitations`. El directorio de la iglesia (`GET /organizations/{churchId}/directory`) no se muestra en Mi espacio: queda en la pantalla Directorio de cada nodo. Solo se agregan `GET /me/nodes` y `GET /me/events`.
7. «Mis eventos y cursos» son aquellos donde el usuario se **inscribió** (por su cuenta o por su correo **verificado**) o **asistió** (la persona vinculada figura en la asistencia). Un correo sin verificar no prueba de quién es y no cuenta.

## User Scenarios & Testing

### US1 — Un miembro sin cargo ve lo básico (P1)

1. **Given** una persona vinculada con membresía en el grupo G y sin cargo, **When** pide sus permisos en G o en un subgrupo, **Then** son `org.read` y `events.read`.
2. **Given** lo mismo, **When** pide permisos en un grupo hermano o en la sede, **Then** no tiene ninguno (403).
3. **Given** el mismo miembro, **When** intenta ver el listado de accesos o las personas del nodo, **Then** 403.
4. **Given** una persona con un cargo de nivel Líder, **Then** conserva los permisos del Líder (el piso no los reduce).
5. **Given** un usuario sin persona vinculada, **Then** no tiene acceso a nada.

### US2 — Mis grupos y ministerios (P1)

`GET /me/nodes` devuelve los nodos donde la persona vinculada al usuario es miembro o líder, con el nombre de la iglesia, si lidera y los cargos que tiene allí. Nunca devuelve los de otras personas.

### US3 — Mis eventos y cursos (P1)

`GET /me/events` devuelve los eventos y cursos donde me inscribí o asistí, el más reciente primero, con una marca por cada origen y el enlace para abrirlos.

### US4 — Mi espacio en la UI (P1, repo `ovejafiel-ui`)

Ruta `/portal/me` para cualquier usuario con sesión, con las secciones Mis eventos y cursos, Mis grupos y ministerios, Directorio, Compartidos contigo e Invitaciones; enlazada desde el selector/menú y destino por defecto de quien solo es miembro. Un miembro común no ve «Accesos» ni botones de administración.

## Fuera de alcance (deuda técnica)

Ver la tabla de `docs/redesign.md` §10: la ocurrencia de una serie y las inscripciones de gente sin ficha siguen como estaban (ítems 17 y 18).
