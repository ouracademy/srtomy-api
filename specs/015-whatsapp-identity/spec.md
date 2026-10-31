# Feature Specification: Quién escribe por WhatsApp

**Feature Branch**: `ccr-fa8547e4-608mjk`

**Created**: 2026-10-31

**Status**: Implemented (API); el agente que la usa va en el repo `srtomy-agent`

**Input**: El bot de WhatsApp (repo `srtomy-agent`) debe servir a los miembros de una iglesia, y la mayoría no tiene cuenta en el portal. Los permisos de esta API dependen de un usuario que inicia sesión, así que hace falta saber quién escribe y darle al bot un modo de llamar a la API **como esa persona**, con sus permisos y nada más. Plan: `/root/.claude/plans/root-claude-uploads-d3014ff9-50e4-52ad-inherited-sunbeam.md`.

## Decisiones

Las tomó el dueño del producto el 31-oct-2026, salvo las marcadas.

0. **Tres niveles, de menos a más** (dueño, 31-oct-2026): (a) **cualquiera**, aunque no esté registrado, puede hacer preguntas básicas de la iglesia: solo lo que la iglesia publica en su página pública (horarios, dirección, teléfono, redes, equipo); sin token. (b) **Quien se identifica por su teléfono** accede a más cosas, a nivel Miembro. (c) **Quien tiene cuenta y verificó su WhatsApp** (fase siguiente) actúa con todos sus permisos.
1. **También sirve a miembros sin cuenta.** Quien no tiene cargos tiene un nivel por defecto: el de **Miembro** (`org.read`, `events.read`) en los nodos donde participa. Ese nivel ya existía (vista `user_access`, spec 008); no se inventa un cargo ficticio.
2. **El teléfono identifica, pero no eleva.** El número viene de WhatsApp, y la iglesia se deduce del número al que escribió. Pero el teléfono de una ficha lo teclea alguien y puede estar mal: por eso quien se identifica solo por teléfono recibe **únicamente el nivel Miembro**, aunque su persona tenga cargos (esta spec). *Decisión de la spec.*
3. **Usuario sombra.** A la persona sin usuario se le crea un usuario con `auth0_sub = 'whatsapp:<personId>'`, vinculado a su iglesia. La vista `user_access` excluye a esos usuarios de la rama de cargos.
4. **Quien ya tiene cuenta real** no se identifica solo por teléfono (`NEEDS_VERIFICATION`). Vincular su WhatsApp con un código desde el portal queda para una spec posterior; hasta entonces el bot no actúa con sus permisos. **Resuelto en la spec 016** (la persona envía un código desde su WhatsApp). *Decisión de la spec.*
5. **Token delegado**: JWT firmado por esta API (RS256), `sub` = el usuario de la persona, `act.sub` = `srtomy-agent`, 10 minutos, misma audiencia que Auth0. Los endpoints normales lo aceptan, así los permisos son los de siempre. El agente pide uno por mensaje.
6. **Solo el agente puede pedirlo**: `/v1/internal/whatsapp/**` acepta el ID token de Google de la cuenta de servicio del agente (`internal.agent-email`) y nada más; esa cuenta no puede llamar al resto de `/v1/internal/**`, ni el scheduler a esta ruta.
7. **Baja**: ante «STOP» el agente la registra y la persona deja de recibir avisos (`whatsappConsent=false`, spec 014).
8. **Cuenta real después**: si la persona crea una cuenta y se vincula, el usuario sombra se elimina y la cuenta real ocupa su lugar.
9. **Fuera de alcance**: el bucle del agente, la vinculación verificada de quien tiene cuenta (decisión 4), y mostrar el número de la iglesia.

## User Scenarios & Testing

### US1 — Un miembro sin cuenta escribe (P1)
1. **Given** una persona de la iglesia con su teléfono en la ficha y sin cuenta, **When** escribe al número de la iglesia, **Then** el agente recibe `LINKED` con un token de 10 minutos y su nombre.
2. **Given** ese token, **When** el agente lee el grupo de la persona, **Then** 200; **When** lee las personas o crea una, **Then** 403.
3. **Given** un teléfono sin ninguna persona, **Then** `UNKNOWN`; con varias personas con el mismo teléfono, `AMBIGUOUS`; a un número que no es de una iglesia conectada, `CHURCH_NOT_CONNECTED`. En ninguno hay token.

### US0 — Cualquiera pregunta por la iglesia (P1)
1. **Given** un número que no es de ninguna persona de la iglesia, **When** escribe, **Then** la respuesta es `UNKNOWN` con el nombre y el `churchSlug` de la iglesia y sin token; el agente responde con `GET /v1/public/organizations/{slug}` sin ninguna credencial.
2. **Given** ese mismo número, **Then** no puede leer personas, grupos ni nada que no sea público.
3. **Given** que la iglesia no hizo pública su página, **Then** el agente no tiene qué responder y lo dice (la página pública da 404): lo que se publica lo decide la iglesia.

### US2 — Un líder sin cuenta no hereda su cargo por teléfono (P1)
1. **Given** una persona con cargo de administrador y teléfono, sin cuenta, **When** escribe, **Then** recibe el nivel Miembro: no puede leer personas ni escribir, y `user_access` no tiene filas `POSITION` para su usuario sombra.

### US3 — Quien tiene cuenta (P1)
1. **Given** una persona con cuenta real, **When** escribe, **Then** `NEEDS_VERIFICATION` y no hay token.
2. **Given** un usuario sombra, **When** la persona se vincula luego a una cuenta real, **Then** el usuario sombra desaparece y la próxima vez es `NEEDS_VERIFICATION`.

### US4 — Pedir que paren (P2)
1. **Given** una persona con ese teléfono, **When** el agente registra la baja, **Then** `whatsappConsent` queda en falso para todas las personas con ese teléfono en esa iglesia.

## Requirements

- **FR-001**: `POST /v1/internal/whatsapp/identity {phoneNumberId, from}` responde `{status, token?, expiresAt?, personId?, churchId?, churchName?, churchSlug?, name?}`; `churchName` y `churchSlug` vienen siempre que el número sea de una iglesia conectada (incluso en `UNKNOWN`, `AMBIGUOUS` y `NEEDS_VERIFICATION`); `token`, `personId` y `name` solo en `LINKED` con `status` ∈ `LINKED | CHURCH_NOT_CONNECTED | UNKNOWN | AMBIGUOUS | NEEDS_VERIFICATION`.
- **FR-002**: la iglesia sale de `whatsapp_connection.phone_number_id` (solo conexiones `CONNECTED`); las personas, de su árbol de nodos, activas, comparando **dígitos**: iguales, o el guardado (≥ 8 dígitos, sin ceros a la izquierda) es el final del número de WhatsApp.
- **FR-003**: `POST /v1/internal/whatsapp/opt-out {phoneNumberId, from}` pone `whatsappConsent=false` y `POST /v1/internal/whatsapp/opt-in` (ALTA) lo vuelve a poner en verdadero; ambos responden `{people}`.
- **FR-004**: el token lleva `iss=https://api.srtommy.com/delegated`, `aud=https://api.srtommy.com`, `sub`, `act.sub=srtomy-agent`, `iat`, `exp=iat+10min`; se rechaza si falta `act`, si vence o si lo firmó otra llave. Sin `delegation.private-key` no se emite ni se acepta ninguno.
- **FR-005**: la vista `user_access` no concede la rama `POSITION` a usuarios `whatsapp:%`.
- **FR-006**: `LinkUserToPerson` reemplaza al usuario sombra de la persona; una persona vinculada a una cuenta real sigue dando error.
- **FR-008**: nada de lo que se responde al nivel «cualquiera» sale de otra fuente que la página pública de la iglesia; la lista de lo permitido a ese nivel es explícita en el agente, no un patrón, para que un endpoint público nuevo no se cuele sin revisarlo.
- **FR-007**: ningún registro de log lleva teléfonos ni texto de mensajes.

## Success Criteria

- **SC-001**: un miembro sin cuenta es reconocido en el primer mensaje, sin pasos previos; y cualquier persona, conocida o no, recibe respuesta a una pregunta básica de la iglesia.
- **SC-002**: ningún token delegado concede más que el nivel Miembro; verificado contra el esquema real con una persona de rol administrador.
- **SC-003**: ninguna llamada sin el ID token de Google de la cuenta del agente obtiene un token (401 sin token, 403 con la cuenta del scheduler).
- **SC-004**: `./mvnw -B -ntp verify` y `python3 scripts/check-language.py` en verde.

## Assumptions

- Los teléfonos de las fichas se guardan con el formato que sea; la comparación por dígitos tolera espacios, guiones, `+` y el cero de marcación.
- La página pública hoy no lista eventos próximos: «¿cuándo es el próximo culto?» se responde con el `schedule` de la página. Un listado público de eventos (los compartidos con acceso general) sería una spec aparte.
- Un mismo teléfono compartido (por ejemplo una familia) es `AMBIGUOUS`; el agente no elige.
- El nivel Miembro alcanza para consultar y para flujos públicos; ampliar lo que un miembro puede hacer por WhatsApp (por ejemplo inscribirse a un evento) se hace dándole ese permiso al rol Miembro, no en esta spec.
- Producción necesita: secreto `whatsapp-delegation-key` (PKCS#8 RSA en PEM) y la variable `INTERNAL_AGENT_EMAIL` con la cuenta de servicio del agente.
