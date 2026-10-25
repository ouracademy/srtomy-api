# Implementation Plan: Compartir eventos y cursos

**Spec**: [spec.md](./spec.md)

## Diseño

1. **Dominio** (`event`): `GeneralAccess`, `LinkRole`, `ShareRole`, `AccessLevel`; `Event.shareVia` y `renewShareLink`; entidades `EventShare` y `EventRegistration`.
2. **`EventAccess`** (componente de consulta): el nivel de acceso es el más alto entre el árbol (`events.write` / `events.read`), un compartido con el correo verificado y `CHURCH` siendo de la iglesia. También decide si un visitante puede inscribirse.
3. **`authorize_event`** reemplaza el control por organización en `EventController` (ver: `VIEWER`, editar: `EDITOR`, borrar: `MANAGER`).
4. Comandos: `set_event_sharing`, `renew_event_link`, `share_event` (con aviso por correo), `unshare_event`, `register_for_event`, `cancel_event_registration`. Consultas: `get_event_sharing`, `list_event_registrations`, `get_shared_event`, `list_events_shared_with`.
5. `EventSharingController`. En `SecurityConfig`, `GET /v1/public/**` ya era público (005) y se agrega `POST /v1/public/events/*/registrations`; con token, la página compartida considera lo que el usuario ya tiene.

## Constitution Check

| Principio | Cómo |
| --- | --- |
| II. Tests | Pruebas unitarias de cada handler, del acceso y del controlador; integración sobre el esquema real. |
| III. Seguridad | Enlace de 96 bits renovable; compartidos solo con correo verificado; sin datos de personas en la página pública; la inscripción valida y no duplica. |
| IV. Código en inglés | Sí. |
| V. Simplicidad | Una sola función de nivel de acceso, sin caché. |

## Endpoints

Ver [contracts/http.md](./contracts/http.md).
