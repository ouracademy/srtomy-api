# Feature Specification: Vincular el WhatsApp a la cuenta

**Feature Branch**: `ccr-fa8547e4-608mjk`

**Created**: 2026-10-31

**Status**: Implemented (API); la pantalla del portal y el comando del agente van en `srtomy-ui` y `srtomy-agent`

**Input**: Por teléfono el bot solo reconoce a alguien como Miembro (spec 015), y a quien tiene cuenta ni eso: el teléfono de una ficha no prueba quién escribe. Para que el bot actúe con **todos los permisos de una cuenta**, la persona tiene que demostrar dos cosas a la vez: que es dueña de la cuenta y que es dueña del número.

## Decisiones

1. **Versión sencilla (decidida por el dueño, 31-oct-2026): el código viaja al revés.** La persona inicia sesión en el portal y pide un código; lo **envía ella** desde su WhatsApp al número de la iglesia. Que el mensaje llegue desde ese número es la prueba del número (lo garantiza WhatsApp), y haber visto el código en el portal es la prueba de la cuenta. No hace falta ninguna plantilla de Meta.
2. **Código**: 8 caracteres del alfabeto de Crockford (sin I, L, O, U), mostrado `ABCD-EFGH`, 40 bits, válido 10 minutos, de un solo uso. Uno por usuario: pedir otro reemplaza al anterior. Solo se guarda su hash (SHA-256).
3. **Contra adivinanza**: 5 códigos equivocados desde un mismo número en una hora lo bloquean, **incluso con el código correcto**, y no se dice si un intento estuvo cerca.
4. **Un número pertenece a una sola cuenta por iglesia.** Si otra cuenta lo prueba después, el número se mueve a ella (quien lo prueba con un código nuevo es hoy su dueño).
5. **Quién puede pedir el código**: cualquier usuario con sesión que tenga una persona en esa iglesia (la suya). No se pide permiso sobre el nodo: es sobre la propia cuenta. La iglesia debe tener su WhatsApp conectado.
6. **Efecto**: el número probado va primero en la identidad (spec 015). El token es el de la **cuenta real** (`level: ACCOUNT`), con todos sus permisos; las escrituras siguen pidiendo confirmación en el agente. Si la cuenta o su persona dejan de existir en la iglesia, la prueba deja de valer.
7. **Deshacer**: la persona puede desvincular desde el portal; el número vuelve a ser un simple teléfono de una ficha.
8. **Número que se muestra**: el portal necesita decir a qué número escribir; se lee de Kapso al conectar (`GET /whatsapp/phone_numbers/{id}` → `display_phone_number`). Si Kapso no lo da, la conexión se hace igual y el número queda vacío.

## Trabajo futuro (alternativa anotada)

**Código por plantilla de autenticación.** En vez de que la persona escriba el código, la API se lo envía por WhatsApp con una plantilla `AUTHENTICATION` de Meta («*123456* es tu código de verificación», con botón «Copiar código») y ella lo escribe en el portal. Es el flujo habitual de las apps, pero:
- Meta solo lo permite si el portafolio de la iglesia tiene **verificación de empresa** y un **límite de mensajes de 2.000 o más** (docs de Kapso, «Authentication»). Una iglesia nueva puede tardar semanas.
- Cuesta un mensaje de categoría *authentication* por intento.
- Prueba igual la cuenta y el número (la persona recibe el código en ese WhatsApp y está en el portal), pero el número lo da la persona al teclearlo: hay que proteger el envío (límite por usuario y por número) para que no sirva para mandar mensajes a desconocidos.
Se haría cuando las iglesias tengan la cuenta verificada, o si probar con un mensaje entrante resulta incómodo.

## User Scenarios & Testing

### US1 — Vincular mi WhatsApp (P1)
1. **Given** una persona con cuenta y una iglesia con WhatsApp conectado, **When** pide el código en el portal, **Then** ve `ABCD-EFGH`, el número de la iglesia y que vale 10 minutos.
2. **When** envía `ABCD-EFGH` (o `abcd efgh`) desde su WhatsApp a ese número, **Then** el agente le confirma y, desde el siguiente mensaje, el bot actúa con los permisos de su cuenta.
3. **Given** un código ya usado, vencido, ajeno o inventado, **Then** `INVALID_CODE`.

### US2 — Nadie adivina (P1)
1. **Given** 5 intentos equivocados desde un número, **When** envía el código correcto, **Then** `RATE_LIMITED` y no se vincula nada.
2. **Given** un código de otra iglesia, **Then** `INVALID_CODE` en esta.

### US3 — Desvincular y ver (P2)
1. **Given** un número vinculado, **When** la persona desvincula, **Then** el siguiente mensaje de ese número es `UNKNOWN` (o Miembro si su teléfono está en una ficha).
2. **When** consulta, **Then** ve si está vinculado y el número con todo menos los últimos cuatro dígitos oculto.

## Requirements

- **FR-001**: `POST /v1/organizations/{id}/whatsapp/link-code` (sesión) → 201 `{code, expiresAt, displayNumber}`; 400 sin persona en la iglesia o sin WhatsApp conectado; 401 sin sesión.
- **FR-002**: `GET /v1/organizations/{id}/whatsapp/link` → `{linked, maskedPhone, verifiedAt}`; `DELETE` → 204.
- **FR-003**: `POST /v1/internal/whatsapp/link {phoneNumberId, from, code}` (solo el agente) → `{status, churchName}` con `status` ∈ `LINKED | INVALID_CODE | RATE_LIMITED | CHURCH_NOT_CONNECTED`.
- **FR-004**: el código se normaliza (mayúsculas, sin guion ni espacios, O→0, I y L→1) antes de compararlo; solo se guarda su hash.
- **FR-005**: `POST /v1/internal/whatsapp/identity` devuelve `level: ACCOUNT` y el token de la cuenta cuando el número está probado, antes de buscar por teléfono de fichas.
- **FR-006**: tablas `whatsapp_link_code` (un código por usuario), `whatsapp_identity` (único por iglesia y número) y `whatsapp_link_attempt`.
- **FR-007**: los intentos fallidos se cuentan aunque la llamada termine: un código equivocado se responde, no se lanza como error.

## Success Criteria

- **SC-001**: una persona vincula su WhatsApp en menos de un minuto desde que abre la pantalla.
- **SC-002**: tras la prueba, el bot lee las personas de la iglesia a nombre de un administrador; antes, no.
- **SC-003**: ningún número obtiene el acceso de una cuenta sin haber enviado un código válido desde él; verificado contra el esquema real, con intentos equivocados, código vencido, código de otra iglesia y código reutilizado.
- **SC-004**: `./mvnw -B -ntp verify` y `python3 scripts/check-language.py` en verde.

## Assumptions

- El número que WhatsApp informa como remitente es fiable (Meta lo autentica); la API del agente es interna y está protegida (spec 015).
- Un teléfono compartido por varias personas puede vincularse a una sola cuenta por iglesia; las demás siguen como `AMBIGUOUS`/Miembro.
- Falta, en `srtomy-ui`, la pantalla «Vincular mi WhatsApp» (pedir el código, mostrarlo con el número y un enlace `wa.me`, ver el estado y desvincular), y en `srtomy-agent`, reconocer el mensaje «VINCULAR <código>» y llamar al endpoint interno.
