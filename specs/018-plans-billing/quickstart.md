# Cómo usar los planes y los pagos

## En local, sin cobrar a nadie

Sin `dlocalgo.api-key` la API usa la **pasarela de prueba**: el «checkout» es la misma pantalla de vuelta del portal y todo pago cuenta como pagado.

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
TOKEN=...   # un token de Auth0 del dueño de la iglesia
CHURCH=...  # id de la iglesia (o de cualquier nodo suyo)

# El plan de hoy: límites, uso, funciones y precios por país
curl -H "Authorization: Bearer $TOKEN" localhost:8080/v1/organizations/$CHURCH/billing

# Pagar un mes desde Perú (o "CO"; months: 1 o 12)
curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"country":"PE","months":1}' localhost:8080/v1/organizations/$CHURCH/billing/checkout

# Al volver del checkout: confirmar los pagos pendientes con la pasarela
curl -X POST -H "Authorization: Bearer $TOKEN" localhost:8080/v1/organizations/$CHURCH/billing/sync

# Historial de pagos
curl -H "Authorization: Bearer $TOKEN" localhost:8080/v1/organizations/$CHURCH/billing/invoices
```

## Con el sandbox de dLocal Go

1. Crea una cuenta en dLocal Go y copia la API key y la secret key del sandbox.
2. Configura `dlocalgo.api-key`, `dlocalgo.secret-key`, `dlocalgo.url=https://api-sbx.dlocalgo.com` y `dlocalgo.notification-url=https://<tu-túnel>/v1/webhooks/billing`.
3. `POST /billing/checkout` devuelve el `checkoutUrl` real; ábrelo y paga con una tarjeta de prueba de dLocal Go.
4. dLocal Go llama a `/v1/webhooks/billing`; la API verifica la firma, le pregunta el estado del pago y extiende el plan.

En producción las llaves van en Secret Manager (`dlocalgo-api-key`, `dlocalgo-secret-key`).

## Cambiar precios o límites

Todo está en configuración, con estos valores por defecto:

| Propiedad | Por defecto | Qué es |
| --- | --- | --- |
| `billing.free.max-people` | 50 | Personas del plan gratis |
| `billing.free.max-locations` | 1 | Sedes del plan gratis |
| `billing.free.whatsapp-trial-days` | 15 | Días de prueba de WhatsApp |
| `billing.paid.included-people` | 150 | Personas que cubre el precio base |
| `billing.paid.block-size` | 100 | Tamaño de cada bloque extra |
| `billing.paid.yearly-months-charged` | 10 | Meses que se cobran al pagar un año |
| `billing.paid.grace-days` | 7 | Días de gracia tras vencer |
| `billing.paid.prices.pe.base` / `.extra-block` | 75 / 35 | Soles al mes |
| `billing.paid.prices.co.base` / `.extra-block` | 85000 / 40000 | Pesos colombianos al mes |

## La cuota del asistente de IA (para srtomy-agent)

El agente pregunta **antes** de llamar al modelo y avisa **después** con los tokens. Solo lo acepta la cuenta de servicio del agente (`internal.agent-email`), con un token de identidad de Google cuyo público es `internal.audience`:

```sh
# En dev, como la cuenta del agente (hace falta permiso para suplantarla)
AGENT=$(gcloud auth print-identity-token \
  --impersonate-service-account=srtomy-agent@sr-tommy-dev.iam.gserviceaccount.com \
  --audiences=https://api.srtommy.com/internal --include-email)
API=https://<api-de-dev>

# Antes del modelo: cuenta 1 mensaje si el plan lo permite (channel: PORTAL o WHATSAPP)
curl -X POST -H "Authorization: Bearer $AGENT" -H 'Content-Type: application/json' \
  -d '{"nodeId":"'$CHURCH'","channel":"PORTAL"}' $API/v1/internal/billing/ai-usage/reserve
# {"allowed":true,"reason":null,"used":3,"limit":30,"remaining":27,"message":null}
# o {"allowed":false,"reason":"quota","message":"Tu iglesia usó los 30 mensajes gratis..."}

# Después del modelo: el `usage` del AI SDK
curl -X POST -H "Authorization: Bearer $AGENT" -H 'Content-Type: application/json' \
  -d '{"nodeId":"'$CHURCH'","model":"claude-sonnet-5-5","inputTokens":2000,"outputTokens":800,"cacheReadTokens":20000,"cacheWriteTokens":0}' \
  $API/v1/internal/billing/ai-usage/record
# {"costUsd":0.014000,"knownModel":true}
```

- `reason` es `quota` (mensajes del mes), `spend` (costo estimado del mes) o `plan` (WhatsApp sin plan ni prueba). Con `allowed: false` el agente contesta `message` (o su propio texto) y **no** llama al modelo. Cualquier otra respuesta (404 de un nodo que no existe, 5xx, sin conexión) también es «no».
- Los comandos (STOP, BAJA, ALTA, VINCULAR) no llaman a `reserve`: nunca se limitan.
- srtomy-agent ya lo hace así (`src/quota.ts`, `src/chat/route.ts`, `src/whatsapp/service.ts`): en el portal, `POST /chat` responde **402** con `{error, code: "ai.quota" | "ai.spend" | "ai.plan"}` (503 si la API no responde) y por WhatsApp contesta el `message`. El ritmo (`user:<sub>` 20/min, `church:<id>` 60/min, `phone:<hash>` de desconocidos 10/h y 30/día) está en la tabla `rate_limit` de su Postgres.
- `GET /billing` trae `ai: {month, used, limit, costUsd, maxCostUsd}` para el portal.
- En local la API rechaza estas llamadas (no hay cuenta de Google); se prueban con `./mvnw test -Dtest=AiUsageIntegrationTests`.

| Propiedad | Por defecto | Qué es |
| --- | --- | --- |
| `billing.ai.free.messages` | 30 | Mensajes al mes del plan gratis |
| `billing.ai.paid.messages` | 300 | Mensajes al mes del plan Iglesia con 150 personas |
| `billing.ai.paid.messages-per-extra-block` | 100 | Mensajes más por cada bloque de 100 personas sobre 150 |
| `billing.ai.free.max-cost-usd` | 1 | Tope de gasto del mes en el plan gratis, en dólares |
| `billing.ai.paid.max-cost-share` | 0.40 | Tope del plan Iglesia: parte de lo que paga al mes |
| `billing.ai.usd-per-unit.pe` / `.co` | 0.27 / 0.00025 | Dólares por sol y por peso, para comparar el precio con el costo |
| `billing.ai.prices.<modelo>.input` / `output` / `cache-read` / `cache-write` | Lista de Anthropic: Sonnet 5.5 2 / 10 / 0,10 / 2,50; Haiku 5.5 0,10 / 0,50 / 0,01 / 0,125; Opus 5.5 4 / 20 / 0,20 / 5 | Dólares por millón de tokens |
| `billing.ai.default-model` | `claude-sonnet-5-5` | Precio de un modelo sin precio propio |
| `billing.ai.zone` | `America/Lima` | Hora con la que empieza cada mes |

Son propuestas: hay que medir una semana con las trazas de LangSmith (tokens por iglesia) antes de fijarlas, y el precio de Claude en Vertex AI puede no ser el de la lista de Anthropic.

## Recordatorios de vencimiento

Cloud Scheduler llama cada día a `POST /v1/internal/billing/remind` (job `billing-renewal-reminders`, 8:15 en Lima). Se envía un correo por el outbox a cada persona con `billing.manage` de la iglesia:

| Aviso | Cuándo |
| --- | --- |
| `SEVEN_DAYS` | `paidUntil` es dentro de 7 días |
| `ONE_DAY` | `paidUntil` es mañana |
| `GRACE` | `paidUntil` fue ayer (empiezan los días de gracia) |

Cada aviso se envía una sola vez por periodo (`billing_reminder`); correr el job dos veces no repite nada, y pagar otro periodo vuelve a empezar los avisos. En local, el correo solo se escribe en el log.

## Ritmo por IP en los endpoints públicos

Inscribirse con el enlace, cancelar la inscripción, marcar asistencia con el QR y el libro de reclamaciones aceptan **30 llamadas por minuto por IP** (`rate-limit.public.per-minute`), compartido entre instancias (Bucket4j, tabla `rate_limit_bucket`). Más allá responden **429** con `Retry-After` y `code: "rate.limit"`. La IP es la última de `X-Forwarded-For` (la que agrega Cloud Run). `rate-limit.enabled=false` lo apaga (así corren las pruebas).

## Infraestructura

- Secret Manager: `dlocalgo-api-key` y `dlocalgo-secret-key` (`infra/terraform/modules/db-secrets`). Aplica con `TF_VAR_dlocalgo_api_key` y `TF_VAR_dlocalgo_secret_key` (en dev, las del sandbox) y redespliega.
- Los workflows de despliegue fijan `DLOCALGO_URL` (sandbox en dev, producción en prod) y `DLOCALGO_NOTIFICATION_URL=<DEV_API_URL o PROD_API_URL>/v1/webhooks/billing`.

## En el portal (srtomy-ui)

- **«Plan y pagos»** (`/portal/<nodo>/billing`, en el menú para quien tiene `billing.manage`): plan, uso (personas, sedes, mensajes de IA), funciones, precio para sus personas en Perú y Colombia, «Pagar 1 mes» y «Pagar 1 año» (`POST /billing/checkout` y va al checkout), y el historial. Al volver del checkout con `?pago=completado` llama a `POST /billing/sync` una vez.
- **Avisos**: cualquier 402 de la API se muestra con su texto y un botón «Ver planes» (`PlanLimitNotice`); el asistente muestra su propio aviso con el código `ai.*`; «Personas» avisa al 80 % del máximo y «WhatsApp» los días que quedan de prueba (`PlanUsageNotice`).
- **Landing**: los dos planes y una calculadora de precio por personas y país (`app/landing/content.ts`; los números se copian a mano de la API, ver la fila 27 de la deuda técnica del portal).
- Culqi ya no existe en el portal.

## Para quien escribe un módulo nuevo

Una función del plan Iglesia se protege así, en el handler, antes de cambiar nada:

```java
checkFeatureQueryHandler.handle(new CheckFeatureQuery(organizationId, Feature.FINANCES));
```

Sin el plan, la API responde **402** con `code: "feature.finances"` y un mensaje en español. Para un máximo de cantidad, `CheckCapacityQueryHandler` con `PEOPLE` o `LOCATIONS`. Si la función es nueva, agrégala a `billing.domain.Feature` con su nombre en español.

## Qué ve la iglesia

| Respuesta | Cuándo | `code` |
| --- | --- | --- |
| 402 | Gratis con 50 personas y registra otra | `limit.people` |
| 402 | Gratis con 1 sede y abre otra | `limit.locations` |
| 402 | Terminó la prueba de WhatsApp y pide el enlace | `feature.whatsapp` |
| 402 | Usa una función del plan Iglesia sin tenerlo | `feature.<nombre>` |
| 429 | Demasiadas llamadas a un endpoint público desde la misma IP | `rate.limit` |
| 401 | Una notificación de pago sin firma válida | — |
