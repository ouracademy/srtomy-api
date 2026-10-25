# Implementation Plan: Invitaciones

**Spec**: [spec.md](./spec.md)

## Diseño

1. **`notification`** (módulo nuevo): `SendEmailCommandHandler` → `EmailGateway` (`ResendEmailGateway` con `RestClient` de Spring si hay `resend.api-key`; `LoggingEmailGateway` si no). Un fallo del proveedor se devuelve en el resultado, no se lanza.
2. **`invitation`** (módulo nuevo): entidad `Invitation`, comandos `create_invitation`, `accept_invitation`, `revoke_invitation`, consultas `find_invitation_by_id`, `find_invitation_by_token`, `list_invitations_of_organization`, `list_invitations_for_email`.
3. **`iam`**: `AuthorizeAccessGrantQuery` recibe el permiso requerido (por defecto `iam.manage`); la invitación usa `invitations.send`. Es la misma regla de «tener todos los permisos del cargo».
4. **`user`**: `find_linked_person` e `is_person_linked`.
5. **`person`**: campo `email`.
6. **Seguridad**: `GET /v1/public/**` sin token (también lo usarán la 006 y la 007).
7. **Aceptar** crea la ficha con `CreatePerson` (sin cargos, para no exigir `iam.manage` al invitador), la vincula (`LinkUserToPerson`) y agrega el cargo con `CreateAccountability`. La comprobación del invitador se rehace al aceptar.
8. **Reglas compartidas**: `InvitationRules` (al enviar y al aceptar) aplica la anti-escalada al cargo y a los cargos que la persona ya tiene; para eso `accountability` expone `get_person_accountabilities`.
9. **Configuración**: `app.url` (`APP_URL`, origen de los enlaces), `resend.api-key` (`sm@resend-api-key`), `resend.from`.

## Constitution Check

| Principio | Cómo |
| --- | --- |
| II. Tests | Pruebas unitarias de cada handler y del controlador; integración sobre el esquema real. |
| III. Seguridad | Token aleatorio (256 bits) guardado como hash; correo verificado; anti-escalada comprobada al enviar y al aceptar. |
| IV. Código en inglés | Sí; los mensajes al usuario en español. |
| V. Simplicidad | Sin SDK nuevo; sin job de vencimiento. |

## Endpoints

Ver [contracts/http.md](./contracts/http.md).
