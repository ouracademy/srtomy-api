# Implementation Plan: Mi espacio y el nivel Miembro

**Spec**: [spec.md](./spec.md)

## Diseño

1. **Migración `V1.7.0__member_access.sql`**: `CREATE OR REPLACE VIEW user_access` con una cuarta rama `MEMBER` (persona vinculada + `IS_PART_OF` + rol Miembro). Mismas columnas que antes.
2. **`RoleBindingRepository.findAccess`** deja fuera la rama `MEMBER`: la pantalla de accesos solo lista concesiones reales. Las demás consultas (permisos, árbol del usuario, `holdsRole`) la ven como cualquier otra rama.
3. **`directory/query/get_my_nodes`**: compone `FindUserBySubject`, `FindAccessibleOrganizations`, `FindLinkedPerson` y `GetPersonAccountabilities`; solo lee las cuentas de la persona vinculada al llamador.
4. **`event/query/list_my_events`**: una consulta nativa de solo lectura en `EventRepository` sobre `event_registration`, `event_attendance` y `user_person` (el correo solo se usa si está verificado).
5. Endpoints `GET /me/nodes` (`DirectoryController`) y `GET /me/events` (`EventSharingController`), solo con sesión.

## Constitution Check

| Principio | Cómo |
| --- | --- |
| II. Tests | Unitarias de cada handler, de los controladores y de integración sobre el esquema real (`MySpaceIntegrationTests`); se actualizaron las pruebas de IAM e invitaciones que suponían «sin cargo = sin acceso». |
| III. Seguridad | Solo datos propios; el correo sin verificar no cuenta; ninguna persona ni menor de otro aparece; la UI solo oculta y la API autoriza. |
| IV. Código en inglés | Sí. |
| V. Simplicidad | Sin tablas nuevas: una rama más de la vista. |

## Endpoints

Ver [contracts/http.md](./contracts/http.md).
