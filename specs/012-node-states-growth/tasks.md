# Tasks: Estado por nodo, personas en grupos y crecimiento

- [x] T001 Migración `V1.16.0__member_state_events.sql`: tabla `member_state_event` y relleno con lo existente.
- [x] T002 `MemberStateEvent`, su repositorio y `MemberStateEvents`; los hooks en `CreateAccountabilities`, `SetMemberState` y `EndAccountabilitiesOfChild`.
- [x] T003 Comando `set_node_membership` (estado por nodo, también en la iglesia; un cargo exige ser miembro).
- [x] T004 `PUT /people/{id}/memberships/{organizationId}` y `GET /people/{id}/memberships`.
- [x] T005 `GET /people` trae `memberState` y filtra por `memberState`.
- [x] T006 Consulta `get_member_growth` y `GET /organizations/{id}/growth`.
- [x] T007 `PUT /organizations/locations/childs/{id}` ya no reemplaza miembros ni líderes; el listado público no repite a los líderes como miembros.
- [x] T008 Pruebas unitarias por handler y una sola prueba de integración en Postgres (`MembershipIntegrationTests`).
- [x] T011 Quitar el endpoint del tablero (`member-state`) y cambiar los cargos sugeridos («Líder de grupo», «Líder de ministerio»).
- [x] T009 Docs: deuda técnica en `docs/redesign.md`.
- [x] T010 `mvn test`, `spotless:check` y `scripts/check-language.py`.
