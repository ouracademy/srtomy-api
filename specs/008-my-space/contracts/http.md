# HTTP contract: Mi espacio

Todas piden sesión (JWT) y devuelven solo datos del usuario que llama.

| Método y ruta | Descripción |
| --- | --- |
| `GET /me/nodes` | `[{id, name, type, churchId, churchName, leader, positions[]}]`, ordenado por iglesia y nombre. Nodos donde la persona vinculada al usuario es miembro o líder (o tiene un cargo). Vacío sin persona vinculada. |
| `GET /me/events` | `[{title, start, isACourse, organizationName, registered, attended, slug}]`, el más reciente primero. `registered`: inscripción por cuenta o por correo verificado; `attended`: la persona vinculada figura en la asistencia. Se abre en `/e/{slug}`. |

Ya existían y Mi espacio reutiliza:

| Método y ruta | Descripción |
| --- | --- |
| `GET /organizations/{churchId}/directory` | El directorio de la iglesia (006); 403 si el usuario no tiene ningún nodo en ella. |
| `GET /me/shared-events` | Compartidos conmigo (007). |
| `GET /me/invitations` | Invitaciones pendientes (005). |
| `GET /me/organizations`, `GET /me/permissions?organizationId=` | El árbol del usuario y sus permisos; un miembro sin cargo ahora aparece con `org.read` y `events.read`. |
