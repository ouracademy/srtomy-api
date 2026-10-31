# Feature Specification: Conectar el WhatsApp de la iglesia y enviar avisos

**Feature Branch**: `ccr-fa8547e4-608mjk`

**Created**: 2026-10-31

**Status**: Draft

**Input**: El benchmark de apps de iglesias (24-oct-2026) muestra que WhatsApp casi no está cubierto y que ninguna app confirma IA por WhatsApp. El propietario o pastor debe poder conectar el WhatsApp de su iglesia (con Kapso, un proveedor de la API oficial de WhatsApp Business) y que la API envíe avisos por ahí. Plan de referencia: `/root/.claude/plans/root-claude-uploads-d3014ff9-50e4-52ad-inherited-sunbeam.md`. El canal `WHATSAPP` ya existe en el outbox («WhatsApp later», `V1.9.0__outbox.sql`).

## Decisiones

1. **Una conexión por nodo iglesia.** Las sedes hijas usan la de su iglesia mientras no tengan la suya (fuera de alcance conectar una por sede en esta spec).
2. **Quién conecta**: quien tenga el permiso `org.update` sobre el nodo (propietario y administrador). Los demás reciben 403.
3. **Proveedor intercambiable**: la API habla con Kapso solo a través de una interfaz `WhatsAppGateway` (como `EmailGateway`), con una implementación de Kapso, una de registro en log para local y pruebas. Cambiar de proveedor no toca el dominio.
4. **La llave de Kapso solo vive en la API** (Secret Manager: `kapso-api-key` y `kapso-webhook-secret`); nunca llega al navegador.
4b. **Quién paga a Meta** (decidido por el dueño, 31-oct-2026): nosotros, con créditos prepago de Kapso (`meta_billing_mode: partner_managed`), y se lo cobramos a cada iglesia. No se puede cambiar después en esa cuenta de WhatsApp. Consultar el funding, registrar el uso por iglesia y alertar saldo bajo queda para la spec 015; el cobro (Culqi) para otra.
5. **Dos modos de conexión**: *Coexistence* (el pastor sigue usando la app de WhatsApp Business en su celular) o *Dedicated* (solo API). Lo elige el pastor al pedir el enlace.
6. **Avisos que inicia el sistema** (recordatorio de evento, bienvenida) van **siempre con plantilla aprobada por Meta**; una plantilla es un texto fijo con variables. Las respuestas libres dentro de la ventana de 24 h pertenecen al bot (otra spec).
7. **Fuera de alcance**: el bot/agente en Python (`srtomy-agent`), la bandeja de conversaciones, vincular teléfono con persona, una conexión propia por sede, cobrar el costo de Kapso/Meta, y la pantalla del portal (spec `014` de `srtomy-ui`).

## User Scenarios & Testing

### US1 — Conectar el WhatsApp de la iglesia (P1)
1. **Given** una iglesia sin conexión y un propietario, **When** pide el enlace de configuración eligiendo un modo, **Then** recibe una URL de Kapso y la conexión queda `PENDING`.
2. **Given** un enlace pedido, **When** Kapso avisa que el número quedó conectado, **Then** la conexión pasa a `CONNECTED` con el número visible y el modo.
3. **Given** una conexión `PENDING`, **When** se pide otro enlace, **Then** el anterior deja de valer y solo hay uno activo.
4. **Given** un usuario sin permiso de administrar el nodo, **Then** 403; nodo inexistente, 404.

### US2 — Ver el estado y desconectar (P1)
1. **Given** una iglesia con o sin conexión, **When** se consulta, **Then** se ve `NOT_CONNECTED`, `PENDING` o `CONNECTED` (con número y modo).
2. **Given** una conexión `CONNECTED`, **When** el propietario desconecta, **Then** pasa a `DISCONNECTED`, los avisos pendientes de esa iglesia se cancelan y no salen más.
3. **Given** una sede hija sin conexión propia, **When** se consulta, **Then** muestra la de su iglesia y dice que es heredada.

### US3 — Recibir avisos por WhatsApp (P1)
1. **Given** una iglesia conectada y una persona con teléfono, **When** el sistema encola un recordatorio de evento, **Then** se entrega con la plantilla correspondiente y el mensaje queda `SENT` con el id del proveedor.
2. **Given** un reintento del mismo mensaje, **Then** el destinatario no recibe un duplicado (clave de idempotencia = id del mensaje).
3. **Given** un número inválido, no registrado en WhatsApp o una plantilla rechazada, **Then** el mensaje pasa a `FAILED` de inmediato; un fallo transitorio del proveedor se reintenta.
4. **Given** una iglesia sin conexión `CONNECTED`, **Then** el mensaje pasa a `FAILED` con motivo claro y no se reintenta.

### US4 — Mensaje de bienvenida (P2)
1. **Given** una iglesia conectada, **When** se registra una persona nueva con teléfono, **Then** se encola un aviso de bienvenida por WhatsApp (solo si dio su consentimiento, ver FR-009).

## Edge Cases

- El webhook llega repetido o fuera de orden: se aplica una sola vez, por el id del evento.
- Firma del webhook inválida: 401, sin cambiar nada.
- La persona pidió no recibir mensajes: no se encola.
- El pastor se desconecta desde Meta: Kapso avisa y la conexión pasa a `DISCONNECTED`.
- El proveedor está caído: la cola reintenta; si se agotan los intentos, `FAILED`.

## Requirements

- **FR-001**: `POST /v1/organizations/{id}/whatsapp/setup-link` (modo `COEXISTENCE|DEDICATED`) crea, si hace falta, el cliente de la iglesia en el proveedor y el enlace, y devuelve `{url, expiresAt}`; deja la conexión `PENDING`. `{id}` puede ser una sede: la conexión es de su iglesia.
- **FR-002**: `GET /v1/organizations/{id}/whatsapp` devuelve `{status, displayNumber, mode, inherited}`.
- **FR-003**: `DELETE /v1/organizations/{id}/whatsapp` desconecta (`DISCONNECTED`). Los mensajes que aún esperan fallan de inmediato con un motivo claro cuando les toca, porque la iglesia ya no tiene conexión.
- **FR-004**: `POST /v1/webhooks/whatsapp` recibe los avisos del proveedor: **verifica la firma** (HMAC-SHA256 del cuerpo crudo, cabecera `X-Webhook-Signature`) y es idempotente por `X-Idempotency-Key`. Es el único endpoint de esta función fuera del JWT de Auth0; sin secreto configurado rechaza todo. Solo actúa sobre `whatsapp.phone_number.created`; los demás eventos se aceptan y se ignoran.
- **FR-005**: `WhatsAppOutboxHandler` implementa `OutboxMessageHandler` para `WHATSAPP`: lee el payload (`organizationId`, plantilla, variables), usa el número conectado de la iglesia y llama a `WhatsAppGateway.sendTemplate`; devuelve el id del proveedor. Kapso no documenta clave de idempotencia para envíos, así que tras un tiempo de espera agotado un reintento podría duplicar un mensaje (entrega al menos una vez).
- **FR-006**: errores del proveedor que no se arreglan reintentando (4xx salvo 408/409/425/429, número inválido) lanzan `PermanentDeliveryException`; los demás son transitorios.
- **FR-007**: catálogo de plantillas en español (`event_reminder`, `welcome`) versionado en el código, con su categoría (utility) y variables.
- **FR-008**: todo envío pasa por el outbox (nunca dentro de la transacción), como manda `AGENTS.md`.
- **FR-009**: la persona tiene `whatsappConsent` (booleano, **verdadero por defecto**, editable al crear y editar la persona; ausente en la petición conserva el valor actual). Solo se encola un aviso a quien tiene teléfono y `whatsappConsent` verdadero; quien pide parar lo deja en falso.
- **FR-010**: la llave y el secreto del webhook vienen de Secret Manager; en local y pruebas se usan valores falsos.

### Key Entities

- **Conexión de WhatsApp**: pertenece a un nodo iglesia; estado, modo, número visible, id del cliente y del número en el proveedor, fecha de conexión. Una por iglesia.
- **Plantilla**: nombre, categoría, idioma y variables; vive en el código.
- **Mensaje del outbox** (existente): canal `WHATSAPP`, destinatario = teléfono en formato internacional.

## Success Criteria

### Measurable Outcomes

- **SC-001**: un propietario conecta el WhatsApp de su iglesia en menos de 10 minutos desde que abre la pantalla.
- **SC-002**: un aviso encolado llega al miembro en menos de 1 minuto en el 95 % de los casos, sin duplicados aunque haya reintentos.
- **SC-003**: ningún usuario sin permiso puede ver ni cambiar la conexión; ninguna petición con firma inválida cambia datos.
- **SC-004**: `./mvnw -B -ntp verify` y `python3 scripts/check-language.py` en verde, con pruebas unitarias, de controlador y de integración.

## Assumptions

- Hay una cuenta de Kapso de Sr. Tommy con un cliente por iglesia; el costo de Meta lo paga el WABA de cada iglesia.
- Los detalles de la API de Kapso (rutas, cabeceras, firma del webhook) se verificaron en `docs.kapso.ai` el 31-oct-2026. Falta probarlos contra una cuenta real de Kapso (fase 0 del plan).
- El número visible (`displayNumber`) todavía no se llena: el webhook de conexión solo trae `phone_number_id`. Pendiente: leerlo de Kapso o del redirect de éxito.
- Quién escribe al bot (identidad, token y baja) está en la spec 015.
- Pendiente de esta spec: encolar el recordatorio de evento y la bienvenida (US3/US4) desde los comandos de eventos y de personas; hoy existe el canal, el manejador y las plantillas, pero nadie encola todavía.
- Los números de teléfono de las personas ya se guardan o se pueden guardar en formato internacional.
- El teléfono del destinatario es el del campo de contacto existente de `person`.
