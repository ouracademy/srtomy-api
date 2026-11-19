# Feature Specification: Planes, pagos y límites de uso

**Feature Branch**: `claude/church-features-pricing-plan-masj76`
**Created**: 2026-11-16 · **Reemplazada**: 2026-11-19 (el dueño del producto eligió el plan de precios «Recomendado»: cuatro planes en lugar de dos)
**Status**: Decidida por el dueño; quedan abiertas las que dice «Pendiente»
**Input**: «Mejora nuestra forma de pagar, quita Culqi; debemos soportar iglesias de Perú y Colombia. Define un plan gratuito y luego planes de pago. Pon límites a las funcionalidades.» Y después: «enfócate en el rate limiting y los pagos».

## Resumen

Hoy la landing mostraba tres planes que no se cobraban ni se cumplían y el formulario de perfil cargaba Culqi sin usarlo. Esta spec reemplaza eso por:

1. **Cuatro planes**: **Gratis**, **Básico**, **Iglesia** y **Red**. Cada uno tiene su precio y sus topes (personas, sedes, mensajes del asistente de IA y recordatorios por WhatsApp). **No hay cobro por bloque de personas**: al llegar al tope de personas se pasa al plan siguiente. Pagar un año cuesta 10 meses.
2. **Pago con tarjeta y medios locales** de Perú (Yape, tarjeta) y Colombia (PSE, Nequi, tarjeta) con **una sola cuenta de dLocal Go**. Se quita Culqi.
3. **Límites del plan que la API hace cumplir** (personas, sedes, funciones) y que el portal muestra antes de que choques con ellos.
4. **Límites de uso del asistente de IA y de los recordatorios por WhatsApp**: un ritmo contra el abuso y una cuota mensual por plan, más un tope de gasto en dólares, para que el costo del modelo y de Meta no lo pague el producto sin tope.

## Los planes

Precios con **IGV incluido en Perú e IVA incluido en Colombia**.

| | **Gratis** | **Básico** | **Iglesia** | **Red** |
| --- | --- | --- | --- | --- |
| Precio al mes | S/ 0 | S/ 45 · COP 49.000 | S/ 75 · COP 79.000 | S/ 149 · COP 159.000 |
| Un año | — | 10 meses | 10 meses | 10 meses |
| Personas | 50 | 150 | 400 | sin límite |
| Sedes | 1 | 1 | 5 | sin límite |
| Mensajes del asistente de IA al mes (portal y WhatsApp) | 20 | 150 | 400 | 1.200 |
| Tope de gasto de IA al mes (estimado, en dólares) | US$ 0,50 | US$ 2 | US$ 5 | US$ 15 |
| Recordatorios por WhatsApp (plantillas de Meta) al mes | 0 | 50 | 200 | 600 |
| Número de WhatsApp propio de la iglesia | no | extra S/ 49 · COP 49.000 al mes | extra S/ 49 · COP 49.000 al mes | incluido |
| Roles personalizados | no (solo los predefinidos) | sí | sí | sí |
| Exportar a Excel | no | sí | sí | sí |
| Correos a grupos, ofrendas y estados de cuenta, turnos y voluntarios | no | no | sí | sí |
| Grupos, ministerios, eventos, cursos, asistencia, QR, inscripciones, páginas públicas y accesos | sí | sí | sí | sí |

Un año al precio de 10 meses: Básico S/ 450, Iglesia S/ 750, Red S/ 1.490 (COP 490.000, 790.000 y 1.590.000).

**WhatsApp.** Por defecto todas las iglesias usan **un número global de Sr. Tommy**, dentro de su cuota de IA y de recordatorios. Conectar **un número propio** (`setup-link`) exige el plan Red o el extra pagado de Básico e Iglesia; sin él, 402 con `feature.own_number`. **La prueba de 15 días con número propio ya no existe.** Cómo se sabe de qué iglesia es quien escribe al número global **no es parte de esta spec**: lo lleva el dueño aparte.

**Por qué así.** Con estos precios estamos al nivel del mercado (ChMeetings y miiglesia.app empiezan en unos US$ 12; Iglesias App con IA, en US$ 70) y, con Haiku y el número global, el margen supera el 20 % desde unas 6 a 9 iglesias pagando; con un número por iglesia y Sonnet no llegaba al 20 % ni con 60. La decisión salió de la calculadora del dueño (ver [research.md](./research.md)). Meta cobra los mensajes de WhatsApp **a la tarjeta de cada iglesia** cuando usa su número propio (`kapso.billing-mode=customer_managed`); los del número global los paga Sr. Tommy, y por eso tienen cuota.

## User Scenarios & Testing

### Historia 1: Una iglesia nueva usa el plan Gratis y ve sus límites (P1)

**Prueba independiente**: crear una iglesia y abrir `GET /v1/organizations/{id}/billing`.

1. **Given** una iglesia recién creada, **When** pide su plan, **Then** está en `FREE`, usa 1 persona de 50 y 1 sede de 1, tiene 20 mensajes de IA, 0 recordatorios, ninguna de las funciones de pago, y ve los cuatro planes con su precio y si le caben (`fits`).
2. **Given** una iglesia gratis con 50 personas, **When** registra o restaura a una más, **Then** recibe 402 con `limit.people` y un mensaje que nombra el plan Básico; no se guarda nada.
3. **Given** una iglesia en Iglesia con 5 sedes, **When** abre otra, **Then** recibe 402 con `limit.locations` que sugiere el plan Red. La Red no tiene tope.
4. **Given** una iglesia gratis, **When** crea un rol propio, **Then** 402 con `feature.custom_roles` («viene desde el plan Básico»).

### Historia 2: El pastor elige un plan y paga con tarjeta, Yape o PSE (P1)

El pastor elige **plan**, **país** (Perú o Colombia), **periodo** (un mes o un año) y, si el plan lo permite, el **número propio** como extra. Paga en el checkout de dLocal Go y vuelve al portal con el plan activo.

1. **Given** una iglesia gratis con 120 personas, **When** pide pagar un mes de Básico desde Perú, **Then** se crea un pago pendiente de S/ 45 y recibe el enlace del checkout; nada cambia hasta que el pago se confirma.
2. **Given** ese pago confirmado, **When** llega la notificación o el pastor vuelve del checkout, **Then** la iglesia pasa a `BASIC` / `ACTIVE` hasta el último día pagado, con los topes de Básico.
3. **Given** un pago ya acreditado, **When** llega otra vez la misma notificación, **Then** no se suman días.
4. **Given** una iglesia con días pagados, **When** paga otro periodo antes de que terminen, **Then** el nuevo periodo empieza el día siguiente al último pagado. **El plan del último pago rige desde que se acredita**: no hay prorrateo.
5. **Given** una notificación sin la firma de dLocal Go, **Then** 401 sin leerla. Una notificación firmada nunca da días por sí sola: el estado se le **pregunta** a dLocal Go, y un pago de otra orden no se acredita.
6. **Given** una iglesia con 200 personas, **When** pide Básico (150), **Then** 400: «Elige un plan más grande». El plan que no le cabe no se vende.
7. **Given** el plan Red, **When** pide el número propio como extra, **Then** 400: ya lo incluye. **Given** Básico o Iglesia con el extra, **Then** cada mes cuesta S/ 49 (COP 49.000) más.

### Historia 3: El plan vence y la iglesia vuelve a los límites del Gratis, sin perder nada (P1)

1. **Given** una iglesia cuyo último día pagado fue ayer, **Then** está en `GRACE` y conserva su plan 7 días.
2. **Given** que pasaron los 7 días sin pagar, **Then** está en `EXPIRED`: tiene los topes y funciones del plan Gratis. **No se borra ni se oculta nada** de lo que ya tiene; solo no puede agregar más allá de esos topes.
3. Por correo, a quien tiene `billing.manage`: 7 días y 1 día antes del fin y al empezar la gracia.

### Historia 4: Número de WhatsApp propio (P2)

1. **Given** una iglesia gratis, Básico o Iglesia sin el extra, **When** pide el enlace de conexión, **Then** 402 con `feature.own_number`: «Mientras tanto usas el número de Sr. Tommy».
2. **Given** el plan Red, o Básico/Iglesia con el extra pagado y vigente, **Then** recibe el enlace.
3. **Given** que el extra venció, **Then** el número ya conectado no se borra, pero no se puede conectar otro.

### Historia 5: El asistente de IA y los recordatorios tienen un tope por plan (P1)

Cada mensaje que el asistente responde (portal o WhatsApp) cuenta para la cuota mensual de la iglesia, igual en los dos canales.

1. **Given** una iglesia gratis que usó 20 mensajes este mes, **When** alguien le escribe al asistente, **Then** el agente no llama al modelo y responde que se alcanzó el límite del plan y cómo ampliarlo (nombra el plan siguiente).
2. **Given** cualquier iglesia, **When** se escribe STOP, BAJA, ALTA o VINCULAR, **Then** el comando funciona siempre, con o sin cuota.
3. **Given** una persona que envía más de 20 mensajes en un minuto, **Then** recibe 429 con `Retry-After` (portal) o un aviso de esperar (WhatsApp), aunque haya varias instancias.
4. **Given** que el costo estimado del mes supera el tope en dólares del plan, **Then** se corta igual que con la cuota de mensajes.
5. **Given** un número desconocido que escribe muchas veces, **Then** se le limita por número (10 por hora, 30 por día).
6. **Given** que el plan tiene recordatorios (Básico: 50), **When** el trabajo diario encola el recordatorio 51, **Then** no se encola y la inscripción **no se marca** como avisada. El plan Gratis tiene 0.

### Edge Cases

- Un pago rechazado, cancelado o vencido cierra la factura sin dar días.
- Cada pago guarda su plan, su país y su moneda; la suscripción recuerda los del último.
- La cantidad de personas se toma **al crear el pago** y queda guardada; bajar personas no devuelve dinero.
- Sin llaves de dLocal Go (local y pruebas) se usa una pasarela de prueba que acredita todo y no cobra a nadie.
- El mes de las cuotas empieza en la hora de Lima (`billing.ai.zone`).

## Requirements

### Functional Requirements

- **FR-001** La API decide el plan y sus límites; el portal y el agente solo los leen. Toda comprobación vive en el módulo `billing` de srtomy-api.
- **FR-002** Crear o restaurar una persona, y crear una sede, comprueban el tope **del plan de la iglesia** y responden **402** con `code` (`limit.people`, `limit.locations`) y un mensaje en español que sugiere el plan siguiente.
- **FR-003** Las funciones de pago (`Feature`: `MASS_EMAIL`, `FINANCES`, `VOLUNTEERS`, `EXCEL_EXPORT`, `CUSTOM_ROLES`, `OWN_NUMBER`) se comprueban con `CheckFeatureQueryHandler`; sin el plan responden 402 con `feature.<nombre>`.
- **FR-004** Precios, topes y cuotas viven en configuración (`billing.<plan>.*`), no en el código. Solo se paga 1 o 12 meses; 12 cuestan 10. No hay cobro por bloque de personas.
- **FR-005** El pago es **prepago y sin cobro automático**: cada pago extiende `paidUntil` y fija el plan. Siete días de gracia. Avisos de vencimiento por correo, por el outbox.
- **FR-006** La notificación de dLocal Go se verifica (HMAC-SHA256 de la llave API más el cuerpo, con la llave secreta) antes de leerla, y el estado del pago siempre se le pregunta a dLocal Go. Acreditar es idempotente y con bloqueo de fila.
- **FR-007** Pagar y ver los pagos requieren `billing.manage` (Propietario y Administrador). Ver el plan requiere `org.read`.
- **FR-008** Se quita Culqi del portal y la landing muestra los cuatro planes reales.
- **FR-009** El asistente tiene tres capas de límite: **ritmo** por persona, por iglesia y por número desconocido, compartido entre instancias; **cuota mensual** de mensajes por plan; y **tope de gasto** mensual en dólares por plan, más un tope global en la consola de Anthropic como fusible.
- **FR-010** La cuota la cuenta srtomy-api (la única fuente de verdad del plan). El agente pregunta antes de llamar al modelo y avisa del consumo después; nunca guarda planes ni cuotas.
- **FR-011** Los endpoints públicos sin sesión de la API tienen un ritmo por IP.
- **FR-012** Cada recordatorio de WhatsApp por plantilla cuenta contra la cuota mensual del plan, en la misma transacción que lo encola; sin cuota no se encola.
- **FR-013** El modelo del agente es configurable (`ANTHROPIC_MODEL`, `VERTEX_MODEL`); el costo por mensaje usa `billing.ai.prices` de ese modelo.

### Key Entities

- **Subscription** (`billing_subscription`): una por iglesia; plan del último pago, si incluye el número propio extra, país y `paidUntil`. Sin fila = gratis.
- **Invoice** (`billing_invoice`): un pago; plan, extra, país, moneda, monto, personas al cotizar, meses, periodo, estado, id del pago en dLocal Go.
- **AiUsage** (`billing_ai_usage`): por iglesia y mes; mensajes de IA, tokens, costo estimado en dólares y recordatorios de WhatsApp.

## Success Criteria

- **SC-001** Ninguna iglesia supera por la API el tope de personas ni de sedes de su plan.
- **SC-002** Una iglesia peruana y una colombiana completan un pago real en el sandbox de dLocal Go y quedan activas sin intervención manual.
- **SC-003** Ningún mensaje del asistente llega al modelo cuando la iglesia agotó su cuota; los comandos siguen funcionando.
- **SC-004** El costo de IA por iglesia de pago queda por debajo del 50 % de lo que paga (se mide con las trazas de LangSmith).
- **SC-005** Culqi no aparece en el bundle del portal.
- **SC-006** Ninguna iglesia manda más recordatorios de WhatsApp al mes que su plan incluye.

## Decisiones y pendientes

**Decididas por el dueño (2026-11-19):** los cuatro planes, sus precios (con IGV e IVA), topes y cuotas; el número global de WhatsApp como estándar y el propio como extra; Haiku 5.5 como modelo por defecto del agente.

**Pendientes:**

1. **Evaluar Haiku 5.5** antes de cambiar el modelo por defecto del agente: correr las evaluaciones de LangSmith (`pnpm eval` del agente) y, si fallan, usar Haiku para lecturas y Sonnet solo para escrituras. Hoy el agente sigue con Sonnet 5.5 por defecto.
2. **Ruteo del número global** (cómo saber de qué iglesia es quien escribe): lo lleva el dueño en otra sesión. Hoy los recordatorios de evento siguen saliendo solo si la iglesia tiene un número conectado.
3. **Hosting del portal**: las cuentas suponen el portal en **Cloud Run**, no en Vercel Pro; Vercel Hobby no permite uso comercial. Lo resuelve el dueño.
4. **Facturación electrónica** (SUNAT en Perú, DIAN en Colombia): dLocal Go cobra, pero el comprobante lo emite la empresa. ¿Quién lo emite y con qué sistema?
5. **Cobro automático** (débito de tarjeta cada mes): hoy no, porque Yape y PSE no se pueden debitar solos. Se puede sumar más adelante con las suscripciones de dLocal Go solo para tarjeta.
6. **Paquetes extra de mensajes de IA**: hoy solo se sube de plan.
