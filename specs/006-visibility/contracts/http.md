# HTTP contract: Visibilidad

| Método y ruta | Quién | Descripción |
| --- | --- | --- |
| `PUT /organizations/{id}`, `PUT /organizations/locations/childs/{id}`, `POST /organizations/locations/childs` | `org.update` / `org.create_child` | Aceptan `schedule`, `memberVisibility` y `showOrgChart` (opcionales). |
| `GET /organizations/{id}` | `org.read` | Devuelve además `schedule`, `memberVisibility`, `showOrgChart` y `slug`. |
| `GET /organizations/{churchId}/directory` | acceso en la iglesia | `[{id, name, type, parentId, description, schedule, memberVisibility, leaders[]}]`; la iglesia primero. 403 sin acceso, 404 si no es una iglesia. |
| `GET /organizations/{nodeId}/directory-entry` | acceso en la iglesia | `{node, membersVisible, members[], children[]}`. |
| `GET /public/organizations/{slug}` | nadie | `{name, type, description, schedule, address, phoneNumber, socialLinks, leaders[], members[]\|null, positions[], orgChart\|null}`. 404 si el nodo no es `PUBLIC`. |
