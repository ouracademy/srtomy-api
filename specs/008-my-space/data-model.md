# Data model: Mi espacio

Migración `V1.7.0__member_access.sql`. No agrega tablas ni columnas: reemplaza la vista `user_access`.

| Rama de `user_access` | `source` | Nodo | Permisos |
| --- | --- | --- | --- |
| `role_binding` (Propietario) | `DIRECT` | el del binding | los del rol |
| cargo con nivel de la persona vinculada | `POSITION` | donde tiene el cargo | unión de roles y permisos sueltos del cargo |
| `LEADER` de la persona vinculada | `LEADER` | el grupo o ministerio | los del rol Líder |
| **nuevo** `IS_PART_OF` de la persona vinculada | `MEMBER` | el nodo del que es miembro | los del rol Miembro (`org.read`, `events.read`) |

Todo se hereda hacia abajo por `organization.parent_id`, como antes. La rama `MEMBER` queda fuera de `GET /organizations/{id}/access`.
