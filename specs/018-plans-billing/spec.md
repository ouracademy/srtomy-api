# Feature Specification: Planes, pagos y límites de uso

**Feature Branch**: `claude/church-features-pricing-plan-masj76`
**Created**: 2026-11-16
**Status**: Propuesta (pide decisiones del dueño del producto, ver «Decisiones abiertas»)
**Input**: «Mejora nuestra forma de pagar, quita Culqi; debemos soportar iglesias de Perú y Colombia. Define un plan gratuito y luego por cada N usuarios cobrar $X; lo mínimo podría ser 75 soles. Pon límites a las funcionalidades: el gratuito no puede conectar WhatsApp, o sí pero solo 15 días.» Y después: «enfócate en el rate limiting y los pagos».

## Resumen

Hoy la landing muestra tres planes (Semilla, Crecimiento, Red) que no se cobran ni se cumplen: la API no limita nada y el formulario de perfil carga Culqi sin usarlo. Esta spec reemplaza eso por:

1. **Dos planes**: **Gratis** y **Iglesia**. El plan Iglesia cuesta **S/ 75 al mes** (o **COP 85.000** en Colombia) con 150 personas incluidas, y suma **S/ 35 (COP 40.000) por cada 100 personas más**. Pagar un año cuesta 10 meses.
2. **Pago con tarjeta y medios locales** de Perú (Yape, tarjeta) y Colombia (PSE, Nequi, tarjeta) con **una sola cuenta de dLocal Go**. Se quita Culqi.
3. **Límites del plan que la API hace cumplir** (personas, sedes, funciones) y que el portal muestra antes de que choques con ellos.
4. **Límites de uso del asistente de IA** (portal y WhatsApp): un ritmo contra el abuso y una cuota mensual por plan, para que el costo del modelo no lo pague el producto sin tope.

## Los planes

| | **Gratis** | **Iglesia** |
| --- | --- | --- |
| Precio | S/ 0 | S/ 75 al mes (150 personas) + S/ 35 por cada 100 más. En Colombia COP 85.000 + COP 40.000. Un año = 10 meses |
| Personas | Hasta 50 | Sin máximo (el precio sube con ellas) |
| Sedes | 1 | Sin máximo |
| Grupos, ministerios, eventos, cursos, asistencia, QR, inscripciones, páginas públicas, roles y accesos | Sí | Sí |
| Asistente de IA (portal y WhatsApp) | 30 mensajes al mes | 300 mensajes al mes + 100 por cada bloque de 100 personas sobre 150 |
| WhatsApp de la iglesia (Kapso) | **Prueba de 15 días**, una sola vez | Sí |
| Correos a grupos de personas | No | Sí |
| Ofrendas y estados de cuenta | No | Sí |
| Turnos y voluntarios | No | Sí |
| Exportar a Excel | No | Sí |

Ejemplos de precio mensual: 80 personas, S/ 75; 200 personas, S/ 110; 600 personas, S/ 250. Al año: S/ 750, S/ 1.100 y S/ 2.500.

**Por qué así.** Cada iglesia de pago tiene que cubrir lo que nos cuesta (Cloud Run, Neon, Kapso y el modelo de IA) y dejar margen. El precio crece con las personas porque el uso (mensajes de IA, almacenamiento, envíos) crece con ellas. Meta cobra los mensajes de WhatsApp **a la tarjeta de cada iglesia** (`kapso.billing-mode=customer_managed`), así que ese costo no entra en nuestro precio. Por qué dLocal Go y no otra pasarela está en [research.md](./research.md).

## User Scenarios & Testing

### Historia 1: Una iglesia nueva usa el plan gratis y ve sus límites (P1)

Una iglesia que recién se registra queda en el plan Gratis. Ve en «Plan y pagos» cuántas personas y sedes usa de las que incluye, qué funciones son del plan Iglesia y cuánto le costaría.

**Prueba independiente**: crear una iglesia y abrir `GET /v1/organizations/{id}/billing`.

**Acceptance Scenarios**:

1. **Given** una iglesia recién creada, **When** pide su plan, **Then** está en `FREE`, usa 1 persona de 50 y 1 sede de 1, WhatsApp está disponible (prueba sin empezar) y finanzas, correos, turnos y exportación no.
2. **Given** una iglesia gratis con 50 personas, **When** registra o restaura a una persona más, **Then** recibe 402 con el código `limit.people` y un mensaje en español que ofrece el plan Iglesia; no se guarda nada.
3. **Given** una iglesia gratis con su sede, **When** abre una segunda sede, **Then** recibe 402 con `limit.locations`.

### Historia 2: El pastor paga el plan Iglesia con tarjeta, Yape o PSE (P1)

El pastor elige país (Perú o Colombia) y periodo (un mes o un año), paga en el checkout de dLocal Go con el medio que prefiera y vuelve al portal con el plan activo.

**Acceptance Scenarios**:

1. **Given** una iglesia gratis con 120 personas, **When** pide pagar un mes desde Perú, **Then** se crea un pago pendiente de S/ 75 y recibe el enlace del checkout; nada cambia hasta que el pago se confirma.
2. **Given** ese pago confirmado por dLocal Go, **When** llega la notificación o el pastor vuelve del checkout, **Then** la iglesia pasa a `CHURCH` / `ACTIVE` hasta el último día pagado, y los límites del plan gratis dejan de aplicarse.
3. **Given** un pago ya acreditado, **When** llega otra vez la misma notificación o se vuelve a sincronizar, **Then** no se suman días.
4. **Given** una iglesia con días pagados, **When** paga otro periodo antes de que terminen, **Then** el nuevo periodo empieza el día siguiente al último pagado (renovar antes no hace perder días).
5. **Given** una notificación sin la firma de dLocal Go, **Then** se rechaza con 401 sin leerla. Una notificación firmada nunca da días por sí sola: el estado se le **pregunta** a dLocal Go, y un pago de otra orden no se acredita.

### Historia 3: El plan vence y la iglesia vuelve a los límites del gratis, sin perder nada (P1)

**Acceptance Scenarios**:

1. **Given** una iglesia cuyo último día pagado fue ayer, **Then** está en `GRACE` y conserva todo durante 7 días.
2. **Given** que pasaron los 7 días sin pagar, **Then** está en `EXPIRED`: no puede agregar personas sobre 50 ni sedes sobre 1, ni usar las funciones del plan Iglesia. **No se borra ni se oculta nada** de lo que ya tiene.

### Historia 4: Una iglesia gratis prueba WhatsApp 15 días (P2)

**Acceptance Scenarios**:

1. **Given** una iglesia gratis que nunca conectó WhatsApp, **When** pide el enlace de conexión, **Then** empieza su prueba de 15 días (una sola vez) y recibe el enlace.
2. **Given** que la prueba terminó, **When** pide el enlace, **Then** recibe 402 con `feature.whatsapp`. **When** el agente recibe un mensaje para esa iglesia, **Then** solo atiende los comandos (STOP, ALTA, VINCULAR) y contesta que el asistente está en pausa (ver Historia 5).
3. **Given** una iglesia de pago, **Then** conecta sin prueba ni fecha de fin.

### Historia 5: El asistente de IA tiene un tope de uso por plan (P1)

Cada mensaje que el asistente responde (en el portal o por WhatsApp) cuenta para la cuota mensual de la iglesia. Al acercarse al tope el portal avisa; al llegar, el asistente deja de responder preguntas hasta el mes siguiente o hasta que la iglesia pase al plan Iglesia.

**Acceptance Scenarios**:

1. **Given** una iglesia gratis que usó 30 mensajes este mes, **When** alguien le escribe al asistente, **Then** el agente no llama al modelo y responde en español que se alcanzó el límite del mes y cómo ampliarlo.
2. **Given** cualquier iglesia, **When** se escribe STOP, BAJA, ALTA o VINCULAR, **Then** el comando funciona siempre, con o sin cuota.
3. **Given** una persona que envía más de 20 mensajes en un minuto, **Then** recibe 429 con `Retry-After` (portal) o un aviso de esperar (WhatsApp), aunque el servicio tenga varias instancias.
4. **Given** una conversación que gasta mucho (tokens), **When** el costo estimado del mes de la iglesia supera su tope en dólares, **Then** se corta igual que con la cuota de mensajes.
5. **Given** un número desconocido que escribe muchas veces al WhatsApp de una iglesia, **Then** se le limita por número (10 por hora, 30 por día) para que nadie agote la cuota de la iglesia desde afuera.

### Edge Cases

- Un pago rechazado, cancelado o vencido cierra la factura sin dar días.
- Una iglesia que cambia de país entre pagos: cada pago guarda su país y moneda; la suscripción recuerda el último.
- La cantidad de personas se toma **al crear el pago** y queda guardada en la factura: si la iglesia crece a mitad del periodo, el precio nuevo aplica al siguiente pago.
- Bajar personas no devuelve dinero; no hay prorrateo.
- Sin llaves de dLocal Go (local y pruebas) se usa una pasarela de prueba que acredita todo y no cobra a nadie.

## Requirements

### Functional Requirements

- **FR-001** La API decide el plan y sus límites; el portal y el agente solo los leen. Toda comprobación vive en el módulo `billing` de srtomy-api.
- **FR-002** Crear o restaurar una persona, y crear una sede, comprueban el máximo del plan y responden **402** con `code` (`limit.people`, `limit.locations`) y un mensaje en español.
- **FR-003** Las funciones de pago (`Feature`: `WHATSAPP`, `MASS_EMAIL`, `FINANCES`, `VOLUNTEERS`, `EXCEL_EXPORT`) se comprueban con `CheckFeatureQueryHandler`; sin el plan responden 402 con `feature.<nombre>`.
- **FR-004** Precio = base + bloque × ceil((personas − 150) / 100), en la moneda del país. Solo se paga 1 o 12 meses; 12 meses cuestan 10. Precios y límites viven en configuración (`billing.*`), no en el código.
- **FR-005** El pago es **prepago y sin cobro automático**: cada pago extiende `paidUntil`. Siete días de gracia. Avisos de vencimiento por correo (y WhatsApp si la iglesia lo tiene) 7 días y 1 día antes, por el outbox.
- **FR-006** La notificación de dLocal Go se verifica (HMAC-SHA256 de la llave API más el cuerpo, con la llave secreta) antes de leerla, y el estado del pago siempre se le pregunta a dLocal Go. Acreditar es idempotente y con bloqueo de fila.
- **FR-007** Pagar y ver los pagos requieren `billing.manage` (Propietario y Administrador). Ver el plan requiere `org.read`.
- **FR-008** Se quita Culqi del portal (script, tipos, variable `NEXT_PUBLIC_CULQI_API_KEY`) y la landing muestra los dos planes reales.
- **FR-009** El asistente tiene tres capas de límite: **ritmo** por persona, por iglesia y por número desconocido, compartido entre instancias; **cuota mensual** de mensajes por plan; y **tope de gasto** mensual en dólares por iglesia, más un tope global en la consola de Anthropic como fusible.
- **FR-010** La cuota la cuenta srtomy-api (la única fuente de verdad del plan). El agente pregunta antes de llamar al modelo y avisa del consumo después; nunca guarda planes ni cuotas.
- **FR-011** Los endpoints públicos sin sesión de la API (inscripción pública, asistencia por QR, libro de reclamaciones, cancelar inscripción) tienen un ritmo por IP, para que no se puedan inundar.

### Key Entities

- **Subscription** (`billing_subscription`): una por iglesia; país, `paidUntil`, inicio de la prueba de WhatsApp. Sin fila = gratis.
- **Invoice** (`billing_invoice`): un pago; país, moneda, monto, personas al cotizar, meses, periodo (al pagarse), estado, id del pago en dLocal Go.
- **AiUsage** (`billing_ai_usage`, fase 2): por iglesia y mes; mensajes respondidos, tokens de entrada, de salida y de caché, costo estimado en dólares.

## Success Criteria

- **SC-001** Ninguna iglesia gratis supera 50 personas ni 1 sede por la API.
- **SC-002** Una iglesia peruana y una colombiana completan un pago real en el sandbox de dLocal Go y quedan activas sin intervención manual.
- **SC-003** Ningún mensaje del asistente llega al modelo cuando la iglesia agotó su cuota; los comandos siguen funcionando.
- **SC-004** El costo de IA por iglesia de pago queda por debajo del 50 % de lo que paga (se mide con las trazas que ya tienen la iglesia).
- **SC-005** Culqi no aparece en el bundle del portal.

## Decisiones abiertas (para el dueño del producto)

1. **Precios.** ¿S/ 75 con 150 personas y S/ 35 por cada 100 más? ¿COP 85.000 y COP 40.000 en Colombia (al cambio de hoy, alrededor de US$ 20 y US$ 10)?
2. **Cuota de IA.** ¿30 mensajes al mes en Gratis y 300 en Iglesia (+100 por bloque)? Con Sonnet 5.5 estimamos entre US$ 0,02 y 0,05 por mensaje respondido; con 300 mensajes eso es hasta US$ 15 de los ~US$ 20 que paga la iglesia. Recomendamos **medir una semana con las trazas** antes de fijar la cuota, y evaluar **Haiku 5.5** (20 veces más barato) para las preguntas de desconocidos y de solo lectura.
3. **¿Paquetes extra de mensajes de IA** (por ejemplo +500 mensajes) o solo subir de plan? Proponemos esperar a la medición.
4. **Prueba de WhatsApp**: ¿15 días desde que pide el enlace (lo implementado) o desde que el número queda conectado?
5. **Facturación electrónica** (SUNAT en Perú, DIAN en Colombia): dLocal Go cobra, pero el comprobante lo emite la empresa. ¿Quién lo emite y con qué sistema? Esto puede decidir si conviene un proveedor que actúe como vendedor (merchant of record).
6. **Cobro automático** (débito de tarjeta cada mes): hoy no, porque Yape y PSE no se pueden debitar solos y el monto cambia con las personas. Se puede sumar más adelante con las suscripciones de dLocal Go solo para tarjeta.
