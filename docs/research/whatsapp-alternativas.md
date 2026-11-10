# Reducir la barrera de conectar WhatsApp: alternativas y recomendación

Fecha: 2026-11-09. Alcance: solo investigación; no cambia código ni lo desplegado. Los repos `srtomy-ui` y `srtomy-agent` no estaban disponibles en esta sesión: lo que se dice de ellos sale de las specs 014, 015 y 016 de este repo y de la descripción del encargo.

## 1. Resumen

- El problema real no es WhatsApp sino que **la iglesia tenga que crear algo en Meta** (cuenta de Facebook, portafolio, WABA, datos del negocio, verificación del número).
- **Descartar WhatsApp Web no oficial (Baileys / Evolution API) como producto.** Viola los términos de WhatsApp, el riesgo de baneo es real e impredecible, y los datos pastorales quedarían colgando de un número que se puede perder de un día para otro.
- **Mejor relación esfuerzo/impacto**: (1) acompañar el flujo actual (asistente + conexión asistida por nosotros), (2) abstraer un canal genérico y lanzar **chat web en el portal**, (3) **número compartido de Sr. Tommy para demo de ~30 días**, (4) número propio por iglesia para quien continúe.
- El número compartido es lo que más baja la barrera, pero **su encaje con las reglas de Meta no está verificado** (ver sección 6). Hay que confirmarlo con Kapso/Meta antes de construir.

## 2. Qué se conserva y qué se abstrae

Conservable en cualquier alternativa:

- Niveles de identidad (cualquiera / miembro por teléfono / cuenta vinculada) y el token delegado de 10 minutos (spec 015).
- Confirmación en código de toda escritura y que el agente solo actúe con los permisos de quien escribe.
- Outbox transaccional y Cloud Tasks para todo envío externo.
- La interfaz `WhatsAppGateway`; las plantillas versionadas en código.

Hay que abstraer (hoy está atado a "un número = una iglesia" y a WhatsApp):

- `InboundMessage` / `OutboundMessage` genéricos con `channel`, `channelAccountId` (qué número, bot o widget recibió) y `senderId` (teléfono, id de Telegram, sesión web).
- Identidad por canal: hoy `whatsapp_connection.phone_number_id` decide la iglesia y el usuario sombra es `whatsapp:<personId>`. Habría que generalizar a una tabla de enlaces canal-identidad-persona y a un prefijo por canal.
- `WhatsAppGateway` pasaría a ser una implementación de un `MessagingGateway` por canal.
- Privacidad de trazas (FR-007: sin teléfonos ni texto en logs) debe repetirse en cada canal.

## 3. Tabla comparativa

Pasos del pastor y tiempos son **estimaciones mías**; los costos y requisitos citan fuente en la sección 5.

| Alternativa | Pasos del pastor | Requisitos Meta/Facebook | Riesgo baneo / cumplimiento | Costo | Capacidades | Esfuerzo nuestro | Demo |
|---|---|---|---|---|---|---|---|
| A. Baileys / Evolution (QR) | ~3 (escanear QR) | Ninguno | **Alto**: viola términos; baneo impredecible | Servidor propio; sin tarifa Meta | Sin plantillas ni ventana oficiales; grupos sí | Medio-alto (sesiones, reconexión, seguridad) | 1-2 semanas |
| B. Número compartido Sr. Tommy | ~1-2 (enviar `UNIRME <código>`) | Ninguno para la iglesia; WABA nuestra | Medio: calidad y marca compartidas; **encaje con políticas no verificado** | Una cuenta Kapso + tarifas Meta | Todo lo oficial; sin grupos de la iglesia | Medio: tabla de vínculos, enrutamiento, abstracción | 2-3 semanas |
| C. Número aprovisionado por nosotros (Kapso) | ~5-8 aún pasa por Embedded Signup de Meta | Sí: Facebook + portafolio + datos del negocio | Bajo | Plan Kapso + tarifas Meta | Todo lo oficial | Bajo (ya integrado) | Días, pero el pastor sigue en Meta |
| D. Embedded Signup actual, mejor guiado | ~8-12 hoy; menos con acompañamiento | Sí | Bajo | Igual que hoy | Todo; Coexistence conserva la app | Bajo (UI + lista de requisitos) | 1 semana |
| E1. Chat web en el portal | 0 para miembros que ya entran | Ninguno | Bajo | Solo Claude/Vertex | Sin ventana ni plantillas; sin avisos push | Medio (canal web + abstracción) | 1-2 semanas |
| E2. Telegram | ~2 (abrir bot, un clic) | Ninguno | Bajo | Gratis (límites en sección 5) | Botones, grupos; público menos usual | Medio | 1-2 semanas |
| E3. SMS / correo / PWA | 0-2 | Ninguno | Bajo | SMS de pago; correo ya existe (Resend) | Solo avisos, no conversación | Bajo | Correo ya está |
| F. Menú determinista sin LLM | n/a | n/a | Bajo | Reduce costo de modelo | Botones/listas; ver nota | Bajo-medio | 1 semana |
| G. Otros BSP (360dialog, Twilio, Gupshup, WATI) | Igual que C/D | Sí | Bajo | No verificado | Todo | Medio (nuevo gateway) | No investigado a fondo |

Nota F: los mensajes interactivos (botones de respuesta y listas) son parte de la API oficial según lo que sé de memoria; no los volví a verificar en esta ronda.

## 4. Análisis por alternativa

### A. Baileys / Evolution API
Verificado: Baileys es MIT y está activo (2265 commits, 287 issues abiertos); el propio README dice que no está afiliado a WhatsApp, que no avala usos que violen sus términos y que desaconseja el envío masivo o automatizado. Evolution API se declara Apache 2.0 **con condiciones adicionales de marca y aviso de uso**, y GitHub no la clasifica como licencia estándar; una reseña de terceros afirma que desde la 2.4.0 (en versión candidata) cada instancia debe activarse en un servidor de licencias de la fundación. El README del repo solo menciona telemetría anónima, así que esa afirmación **no está confirmada**.
Riesgo: la fuente que cita los términos (sección 3.4, "clientes no autorizados") es un blog de un competidor; las cifras de baneo ("2 a 8 semanas") son suposiciones de vendedores. Lo que sí es sólido: es un cliente no oficial, WhatsApp no lo respalda, y no existe forma de apelar con soporte.
Para un producto de pago con datos pastorales (consejería, necesidades, familias), perder o filtrar la sesión de un WhatsApp personal del pastor es un daño que no se puede deshacer. **Si el dueño lo quiere de todos modos**: solo como modo "laboratorio" opt-in, con un número desechable (nunca el del pastor), sin envíos masivos, con aceptación escrita del riesgo, sin cobrar como servicio garantizado, y sin guardar el contenido de los mensajes.

### B. Número compartido de Sr. Tommy
Cambios en la API: hoy `whatsapp_connection.phone_number_id` identifica la iglesia. Con un número compartido, la iglesia sale de un **vínculo persona-iglesia** creado al enviar `UNIRME <código>` (o por `wa.me/<número>?text=UNIRME%20<código>`); el código identifica la iglesia. Hace falta una tabla de vínculos (teléfono, iglesia, fecha, consentimiento) y que `POST /v1/internal/whatsapp/identity` resuelva por teléfono cuando el número es el compartido. Un teléfono en dos iglesias obliga a preguntar de cuál habla (hoy `AMBIGUOUS` solo cubre varias personas en una iglesia).
Privacidad: el mismo número recibe mensajes de todas las iglesias, por eso el aislamiento debe estar en código (cada llamada con el token de la persona), no en la conversación. Una persona que se une no queda "reconocida" por el solo teléfono de la ficha: el código es el consentimiento.
Riesgos: si una iglesia genera quejas o reportes, baja la calidad del número de **todas**; el marcador de marca es "Sr. Tommy", no la iglesia (menos confianza). Paso a número propio: la conexión por iglesia ya existe; al conectar la suya, los vínculos migran (el miembro recibe aviso del número nuevo).
No verificado: que Meta acepte que un solo número atienda a varias iglesias como clientes de un tercero. Mi búsqueda no encontró una regla que lo permita ni una que lo prohíba (sobre la regla de "un WABA por empresa", solo hallé un resumen de tercero fechado en 2024).

### C. Números aprovisionados por Kapso
Verificado en la documentación de Kapso: el plan Free da un número estadounidense preverificado y gratis (uno por proyecto y una sola vez), que se libera si no tiene mensajes de producción en 30 días; Pro y Platform incluyen números preverificados. La página no cubre el país local, y **el cliente igual pasa por Embedded Signup de Meta**: inicia sesión con Facebook, elige portafolio y WABA, confirma permisos. Por tanto C **no elimina** la barrera de Meta; solo evita el SMS de verificación. Y un número +1 para una iglesia peruana tiene un costo de confianza. Existe una guía de "números locales", que no revisé.
Meta pide información completa del negocio (nombre legal, dirección, teléfono, sitio web HTTPS público) y puede bloquear el envío por revisión de nombre, verificación de negocio o pago, según la guía de Kapso.

### D. Reducir pasos del Embedded Signup
Según la guía de Kapso, no hay forma documentada de **precargar campos** ni de aprovisionar por enlace. Lo que sí podemos hacer: (1) lista previa en la UI ("antes de empezar necesitas: cuenta de Facebook con acceso, nombre legal, dirección, sitio con HTTPS"), (2) una pantalla paso a paso que refleje las pantallas de Meta, (3) modo asistido: videollamada o compartir pantalla donde nosotros guiamos, (4) detectar el estado (`PENDING` por más de X horas) y ofrecer ayuda. Coexistence (el pastor sigue usando la app Business) exige versión 2.24.17 o superior de la app, según una fuente.
Verificación de empresa: sin verificar, las fuentes (todas de proveedores) coinciden en un tope de **250 conversaciones iniciadas por la empresa cada 24 h**; con verificación sube (hay discrepancia: 1000 o 2000). Para recordatorios de una iglesia, 250 al día probablemente alcanza; es una **suposición** que hay que contrastar con el tamaño real de las iglesias. Meta puede además exigir verificación para aprobar el nombre visible.

### E. Otros canales
- **Chat web en el portal**: el agente ya existe; falta el canal. Identidad: sesión autenticada (mejor que WhatsApp). Sirve a líderes y staff que ya entran al portal; no a miembros sin cuenta.
- **Telegram**: sin verificación; el límite de difusión gratuita es ~30 mensajes por segundo, 1 por segundo por chat (FAQ oficial de bots). Poco uso en iglesias hispanas, pero cero fricción para quien lo tenga.
- **Widget en la página pública de la iglesia**: útil para el nivel "cualquiera" (solo contenido público), muy poco riesgo.
- **Correo / PWA**: complemento para avisos, no para conversar.

### F. Menú determinista
Para lo frecuente (próximo culto, inscribirme, mis datos), un flujo con botones evita pagar modelo y evita fallos del LLM. Las respuestas dentro de la ventana de 24 h pasan a facturarse como servicio desde el 1 de octubre de 2026 (con 1000 gratis al mes por número, según Kapso), así que un menú no ahorra tarifa de Meta, solo costo de modelo y riesgo. Recomiendo menú para los 5 casos más usados y modelo para lo abierto.

### G. Otros proveedores
Solo revisé Kapso. 360dialog, Twilio, Gupshup y WATI siguen requiriendo Embedded Signup o equivalente, por lo que no cambian la barrera central; cambiarían de gateway por costo o soporte. No los cotizo: no tengo fuentes primarias.

## 5. Costos y límites (con fuente)

- Kapso (kapso.com/pricing, consultada hoy): Free $0, 2000 mensajes al mes, 1 número, enlaces de conexión incluidos; Pro $25 al mes, 100.000 mensajes, 3 números, $10 por número extra; Platform $299 al mes, 1.000.000 de mensajes, 50 números, $5 extra, **facturación centralizada de Meta**. Las tarifas de Meta se pagan aparte, a costo, sin recargo (USD).
- Kapso FAQ: modos de facturación por WABA; no se puede pasar de créditos de Kapso a tarjeta de Meta. Cuentas no USD llevan margen de cambio; las soluciones con varios socios, 5 % de tarifa gestionada.
- Meta: tarifas por plantilla entregada, por categoría y país. Cifras de EE. UU. citadas por terceros: marketing 0.025 USD, utilidad ≈ 0.0034 USD. **No encontré la tarifa de Perú** en fuente primaria; hay que consultar la tarjeta de tarifas de Meta.
- Telegram: bots gratis; 30 mensajes por segundo en difusión.

## 6. Verificado vs. supuesto

Verificado hoy en fuentes:
- Licencia y descargo de Baileys; licencia declarada de Evolution API (con condiciones de marca).
- Planes y precios de Kapso; pasos del flujo de conexión de Kapso; requisitos de Meta listados por Kapso.
- Cambio de facturación de mensajes de servicio desde 2026-10-01 (según FAQ de Kapso).
- Política de Meta desde 2026-01-15 contra chatbots de propósito general (fuentes periodísticas y de proveedores, no leí el texto de Meta).

Supuestos o no confirmados:
- Que nuestro bot cuente como asistente de un negocio y no como "chatbot de propósito general" bajo los términos de Meta. Es plausible porque solo atiende asuntos de la iglesia, pero **hay que leer el texto vigente de los Términos de la Solución de WhatsApp Business** antes de ofrecerlo con modelo abierto, y más aún en un número compartido.
- Que un número compartido de Sr. Tommy con enrutamiento por código sea aceptado por Meta.
- Que sin verificación de negocio el tope de 250 alcance.
- Que Evolution API 2.4 exija activación en servidor de la fundación.
- Cifras de baneo de Baileys.
- Tiempos hasta la demo y número de pasos (estimaciones).
- Tarifas de Meta para Perú.

## 7. Recomendación por fases

**Fase 0, esta semana, sin construir canal nuevo**
- Lista previa de requisitos y asistente paso a paso en la pantalla de conexión (UI).
- "Conexión asistida": un enlace para agendar una videollamada; nosotros la hacemos con el pastor.
- Medir cuántas iglesias abandonan y en qué pantalla de Meta (para decidir con datos).
- Leer los Términos de WhatsApp Business y confirmar con Kapso (soporte) si un número compartido para varias iglesias es válido y si aceptan Coexistence sin verificar el negocio.

**Fase 1, 1-2 semanas**
- Abstraer el canal (`InboundMessage` / `OutboundMessage`, identidad por canal) detrás del agente y la API sin cambiar el comportamiento de WhatsApp.
- Chat web en el portal con el mismo agente. Es el primer canal sin Meta y sirve de red de seguridad.
- Menú determinista para los casos frecuentes.

**Fase 2, 2-3 semanas, solo si Fase 0 confirma que es válido**
- Demo compartida de ~30 días: número de Sr. Tommy, `UNIRME <código>`, vínculo persona-iglesia con consentimiento, solo nivel Miembro y página pública, sin envíos iniciados por la iglesia salvo plantillas aprobadas de Sr. Tommy.
- Al día 30 (o antes): invitar a conectar el número propio; el vínculo se migra y el compartido deja de atender a esa iglesia.
- Condición de salida: si la calidad del número compartido baja, se pausa la demo.

**Fase 3**
- Número propio por iglesia (flujo actual, con asistencia) como producto de pago.

**Descartar**
- Baileys / Evolution como producto por defecto (riesgo de baneo y de cumplimiento, sin recurso). No hay fase para ello salvo decisión explícita del dueño con las condiciones de la sección 4A.
- Cambiar de proveedor de WhatsApp ahora: no reduce la barrera central.

## 7b. Hallazgo de la prueba real: conectar desde el celular

Lo reportó otra sesión de Claude que probó el setup link de Kapso (no lo reproduje yo). Con Coexistence, el flujo exige escanear un QR con la app WhatsApp Business del teléfono, y Kapso avisa «usa un computador de escritorio». Desde el mismo celular no se puede escanear su propio QR, y la ventana emergente de Meta se cancela al cambiar de pestaña («Falló la configuración: cancelada o no fue autorizada»). Con Dedicated no hay QR (código por SMS), pero el número no puede seguir en la app Business.

Qué funciona 100 % desde el celular, según lo que sé (lo marcado con * es inferencia mía, sin probar):

| Vía | ¿Solo celular? | Comentario |
|---|---|---|
| Número compartido de Sr. Tommy | Sí | El pastor solo envía `UNIRME <código>`. Sin Meta, sin QR. Encaje con Meta sin verificar. |
| Conexión asistida por nosotros | Sí | Nosotros operamos el escritorio por videollamada; el pastor solo aprueba. |
| Número aprovisionado por Kapso (preverificado) | Parcial* | Evita el SMS y el QR, pero sigue el inicio de sesión de Facebook y la ventana de Meta, que es donde se cancela. Hay que probarlo desde el celular. |
| Dedicated (código por SMS) | Sí* | Sin QR, pero el pastor pierde la app Business en ese número, y la ventana emergente puede fallar igual. |
| Coexistence | No | Necesita un segundo dispositivo con pantalla para el QR. |
| Chat web, Telegram | Sí | Ambos funcionan en el navegador o app del celular. |
| Baileys por QR | No* | También necesita escanear desde otro dispositivo; existe vinculación por código numérico, que no verifiqué. |

Conclusión: un número aprovisionado por nosotros quita el SMS y el QR pero **no** la ventana de Meta; el número compartido es lo único que quita todo. Mientras tanto, la conexión asistida por videollamada resuelve el caso del pastor que solo tiene celular. Cambios sugeridos a la fase 0: probar la ventana de Meta desde el celular con un número preverificado, y ofrecer en la UI el enlace para abrir el setup link desde una computadora (o enviarlo por correo).

## 7c. Hallazgo de la prueba real: moneda del WABA y modo `partner_managed`

Lo reportó otra sesión de Claude (no lo reproduje). Con el setup link de Kapso en `partner_managed`, activar los créditos falla con `unsupported_currency`: el WABA de la iglesia de prueba quedó en **PEN**. Resultado: `not_funded`, envíos pausados y respuestas 402 de la API de Kapso.

**Verificado en fuentes**
- Kapso (FAQ de precios): los créditos y cargos del proyecto son en USD; un WABA en USD paga la tarifa publicada de Meta en USD; un WABA no USD "habilitado" paga además un margen de cambio que Kapso muestra en su panel. La página no dice qué monedas están habilitadas, no menciona PEN ni COP, no explica `unsupported_currency` ni da plazos. Tampoco lo cubre su blog de precios.
- Kapso: no se puede pasar un WABA de créditos de Kapso a tarjeta de Meta; hay que crear otro WABA y migrar el número. Sí se pueden reactivar los créditos en el mismo WABA, si Kapso lo permite y Meta acepta.
- Meta (documentación de líneas de crédito, que solo cubre a los Solution Partners): la línea de crédito no se puede cambiar una vez adjunta a la cuenta; para otra línea hay que crear una cuenta nueva. La moneda se indica al compartir la línea y los valores documentados son AUD, EUR, GBP, IDR, INR y USD. PEN y COP no figuran. La zona horaria y la moneda de la cuenta de mensajería no se pueden editar una vez adjunta la línea.

**Sin confirmar** (fuentes secundarias o ausentes)
- Una API de migración de moneda de Meta que traslade las plantillas a un WABA nuevo con otra moneda. Dos blogs la mencionan; no la vi en documentación de Meta.
- Si un Tech Provider (nuestro caso vía Kapso) puede tener línea de crédito propia o si depende del Solution Partner. Un glosario de terceros dice que los clientes de Tech Providers agregan su propio medio de pago.
- Qué monedas habilita Kapso hoy y en cuánto tiempo pondría PEN o COP. No hay respuesta pública; hay que preguntar a Kapso (el blog lista dev@kap.so como contacto).
- La cantidad exacta de pasos extra de `customer_managed`.

**Respuestas a las tres preguntas**
1. *¿Sirve `partner_managed` para Perú y Colombia?* Hoy no, por lo observado: la cuenta quedó en PEN y no se pueden activar créditos. Que Kapso habilite PEN/COP depende de ellos y no encontré plazo. Un WABA en USD sí entra en el modo sin margen de cambio.
2. *`customer_managed`:* la iglesia pone su tarjeta en Meta (un paso más en el flujo de Meta: medio de pago del WABA). Se puede evitar el bloqueo de la moneda del lado de Sr. Tommy, porque Meta cobra a la tarjeta en la moneda del WABA. Lo que no se puede es esconder la moneda de Meta al pastor; la verá en el formulario de pago. Ventaja: los primeros 1.000 mensajes de servicio al mes por número son gratis (FAQ de Kapso) y no hay riesgo de que Sr. Tommy financie los envíos. Costo: más fricción y una tarjeta que a veces el pastor no tiene en la moneda o con límite internacional (supuesto).
3. *Moneda fijada al crear el WABA:* si la moneda solo se cambia creando un WABA nuevo, el onboarding debe fijar la moneda al principio, no corregirla después. Con un **número compartido o aprovisionado por nosotros con WABA propio en USD** el problema desaparece: el WABA es de Sr. Tommy, los créditos de Kapso son en USD y no hay margen de cambio. Costo: los envíos se facturan en USD a nosotros y se re-facturan a la iglesia (en soles, por Culqi u otro), con riesgo de tipo de cambio. Ese es un argumento más a favor del número compartido en la demo.

**Riesgos**
- Cada iglesia con WABA en PEN/COP queda sin envío hasta que Kapso habilite la moneda o se migre a `customer_managed` (otro WABA nuevo, porque no se cambia el modo).
- Un WABA creado por error en la moneda equivocada obliga a migrar número y plantillas.
- Cambiar el modo de facturación de iglesias ya conectadas cuesta un WABA nuevo y reaprobar plantillas (revisión de hasta un día, spec 014).

**Acciones sugeridas**
1. Preguntar a Kapso por escrito: monedas habilitadas, plazo para PEN y COP, y si el setup link permite elegir USD al crear el WABA.
2. Mientras tanto, para pruebas y la demo, usar un WABA propio en USD.
3. Documentar en la UI qué modo de pago aplica a cada país.

## 8. Preguntas abiertas para el dueño del producto

1. ¿Aceptamos que la demo use una marca compartida ("Sr. Tommy") en vez del nombre de la iglesia?
2. ¿Qué tolerancia hay a que la calidad del número compartido afecte a todas las iglesias?
3. ¿Cuántos miembros activos tiene una iglesia típica? Define si 250 conversaciones al día sin verificar alcanzan.
4. ¿Se hará la verificación de negocio de Sr. Tommy como proveedor (una sola vez)? ¿Quién tiene los documentos legales?
5. ¿Prefieren el número +1 gratuito de Kapso para pruebas internas, o hace falta un número peruano?
6. ¿Quién paga la videollamada de conexión asistida: es parte del plan o un servicio aparte?
7. ¿Se quiere ofrecer algún modo "laboratorio" con QR a quien lo pida, aceptando por escrito el riesgo?
8. Pendientes que esta ronda no agrega a la tabla de deuda técnica de `docs/redesign.md` (sección 10): abstracción del canal y generalización de la identidad. Se proponen para su aprobación, no se anotaron.

## Fuentes

- Kapso: [precios](https://kapso.com/pricing), [FAQ de precios](https://docs.kapso.ai/docs/whatsapp/pricing-faq), [instant setup](https://docs.kapso.ai/docs/platform/phone-numbers/instant-setup), [conectar WhatsApp](https://docs.kapso.ai/docs/how-to/whatsapp/connect-whatsapp)
- [Baileys (GitHub)](https://github.com/WhiskeySockets/Baileys), [Evolution API (GitHub)](https://github.com/evolution-foundation/evolution-api)
- [Riesgo de baneo, Pipes.bot](https://www.pipes.bot/blog/baileys-vs-whatsapp-cloud-api-ban) (fuente con interés comercial)
- [Política de chatbots de WhatsApp, TechCrunch](https://techcrunch.com/2025/10/18/whatssapp-changes-its-terms-to-bar-general-purpose-chatbots-from-its-platform) y [respond.io](https://respond.io/blog/whatsapp-chatbot-policy-2026)
- [Multi-Solution Conversations, Meta](https://developers.facebook.com/documentation/business-messaging/whatsapp/solution-providers/multi-solution-conversations)
- [Coexistence, Meta](https://developers.facebook.com/documentation/business-messaging/whatsapp/embedded-signup/onboarding-business-app-users)
- [Límites de mensajería, Infobip](https://www.infobip.com/docs/whatsapp/manage-integration)
- [Preguntas frecuentes de bots, Telegram](https://core.telegram.org/bots/faq)
