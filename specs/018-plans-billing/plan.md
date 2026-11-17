# Implementation Plan: Planes, pagos y límites de uso

**Spec**: [spec.md](./spec.md) · **Research**: [research.md](./research.md) · **Guía**: [quickstart.md](./quickstart.md)
**Repos**: srtomy-api (todo lo que decide), srtomy-agent (respeta la cuota y el ritmo), srtomy-ui (muestra y cobra).

## Estado

| Fase | Qué | Estado |
| --- | --- | --- |
| 1 | Módulo `billing` en la API: planes, límites, dLocal Go, prueba de WhatsApp | **Hecho** en la rama `claude/church-features-pricing-plan-masj76` de srtomy-api, sin PR |
| 2 | Cuota y tope de gasto del asistente (API + agente) y ritmo compartido | **Hecho**: API (2a) y agente (2b, 2c: `src/quota.ts`, `PostgresRateLimiter`) |
| 3 | Portal: pantalla «Plan y pagos», avisos de límite, landing, quitar Culqi | **Hecho** en srtomy-ui (`app/portal/[nodeId]/billing/`, `PlanLimitNotice`, `PlanUsageNotice`, landing con calculadora) |
| 4 | Infra y avisos: secretos de dLocal Go, recordatorios de vencimiento, ritmo por IP en la API | **Hecho** en la API (falta aplicar Terraform y probar en el sandbox de dLocal Go); el aviso por WhatsApp queda pendiente |

## Arquitectura

```
Portal ──GET /billing──▶ srtomy-api (billing) ◀──POST /webhooks/billing── dLocal Go
   │  POST /billing/checkout ──▶ crea Invoice ──▶ dLocal Go (checkout) ──▶ el pastor paga
   │                                     ▲
   └─ chat ─▶ srtomy-agent ──POST /internal/billing/ai-usage/{reserve,record}──┘
                 └─ ritmo: Postgres compartido (por persona, iglesia, número)
```

- **La API es la única que decide.** `billing.query.Entitlements` lee la suscripción de la iglesia (cualquier nodo da la misma) y responde qué permite el plan hoy. Los otros módulos llaman a `CheckCapacityQueryHandler` o `CheckFeatureQueryHandler`; nunca leen tablas de `billing`.
- **402 Payment Required** con `ProblemDetail.code` es la respuesta uniforme a «tu plan no lo incluye» (`PlanLimitException` en `shared/domain`). El portal la reconoce y ofrece el plan Iglesia.
- **Prepago**: cada `Invoice` pagada mueve `Subscription.paidUntil`. Sin trabajo programado para «cobrar»: solo un aviso de vencimiento (fase 4).

## Fase 1: hecha (srtomy-api)

Paquete `org.ouracademy.ovejafiel.billing`:

| Pieza | Archivo |
| --- | --- |
| Precios y límites en configuración | `domain/BillingPolicy`, `domain/BillingPolicyConfig` (`billing.free.*`, `billing.paid.*`) |
| Suscripción, factura, estados | `domain/Subscription`, `domain/Invoice`, `SubscriptionStatus` (`FREE`, `ACTIVE`, `GRACE`, `EXPIRED`) |
| Qué permite el plan | `query/Entitlements`, `query/check_feature`, `query/check_capacity`, `query/get_billing`, `query/list_invoices` |
| Pagar | `command/start_checkout`, `command/sync_invoices`, `command/sync_payment`, `command/InvoiceSettler` |
| Prueba de WhatsApp | `command/start_whatsapp_trial` (la llama `RequestSetupLinkCommandHandlerImpl`) |
| Pasarela | `gateway/DLocalGoPaymentGateway`, `gateway/SandboxPaymentGateway`, `gateway/PaymentGatewayConfig` |
| HTTP | `controller/BillingController`, `controller/BillingWebhookController` |
| Base de datos | `V1.32.0__billing.sql` (tablas y permiso `billing.manage` para Propietario y Administrador) |

Límites conectados: crear persona y restaurar persona (`limit.people`), crear sede (`limit.locations`), pedir el enlace de WhatsApp (prueba de 15 días). Permiso nuevo en el catálogo: `billing.manage`. Los de finanzas y mensajes los agrega la sesión de cada módulo.

Pruebas: dominio (precios, periodos, gracia, prueba), pasarela contra un servidor local (envío, lectura, firma calculada aparte), webhook (firma, rechazos), reglas con fecha fija, y el flujo completo contra Postgres (límites, pago de un mes, renovación de un año desde Colombia, permisos, prueba de WhatsApp).

## Fase 2: límites del asistente de IA

### 2a. Cuota mensual en la API

- Tabla `billing_ai_usage (organization_id, month, messages, input_tokens, output_tokens, cache_read_tokens, cost_usd)`, clave `(organization_id, month)`.
- `POST /v1/internal/billing/ai-usage/reserve` `{nodeId, channel: PORTAL|WHATSAPP}` → `{allowed, remaining, reason}`. Suma 1 mensaje **en el mismo paso** que comprueba (`insert … on conflict do update … where messages < limit returning`), para que dos instancias no se pasen del tope a la vez.
- `POST /v1/internal/billing/ai-usage/record` `{nodeId, inputTokens, outputTokens, cacheReadTokens, model}` → suma tokens y el costo estimado con una tabla de precios en configuración (`billing.ai.prices.<modelo>`).
- `reserve` niega con `reason`: `quota` (mensajes del mes), `spend` (tope en dólares: `billing.ai.max-cost-usd.free` = 1, `.paid` = 40 % del precio del plan) o `plan` (WhatsApp sin plan ni prueba).
- Solo la cuenta de servicio del agente (`InternalSecurityConfig`, ruta `/v1/internal/billing/ai-usage/**` con `callerIs(agentEmail)`).
- `GET /billing` suma `ai: {month, used, limit, costUsd, maxCostUsd}` para que el portal lo muestre.
- **Hecho así**: `billing/command/reserve_ai_message`, `record_ai_usage`, `query/AiAllowance`, `domain/AiPolicy` (`billing.ai.*`, con precios por modelo en `billing.ai.prices.<modelo>`), `controller/AiUsageController`, `V1.33.0__billing_ai_usage.sql`. La negación es un 200 con `allowed: false`, `reason` y `message` en español; un error (404, 5xx) también significa no llamar al modelo. El mes sigue la hora de Lima (`billing.ai.zone`). Si el modelo no tiene precio, se cobra como `billing.ai.default-model`.

### 2b. El agente respeta la cuota

- En `src/chat/route.ts` y `src/whatsapp/service.ts`, después de los comandos (`commands.ts`) y **antes** de llamar al modelo: `reserve`. Si niega, contesta el texto fijo en español y no llama al modelo.
- Al terminar (`onFinish` del AI SDK, con `usage`): `record`. Un fallo de `record` se registra y no corta la respuesta.
- Si la API no responde a `reserve`: **negar** (fallar cerrado), salvo los comandos.

### 2c. Ritmo compartido entre instancias

- `PostgresRateLimiter` implementa la interfaz `RateLimiter` que ya existe (`src/rate-limit.ts`): ventana fija por clave en una tabla `rate_limit (key, window_start, hits)` con `insert … on conflict do update returning`, un solo paso atómico, igual que `seen_key`.
- Claves y límites: `user:<sub>` 20 por minuto (hoy), `church:<id>` 60 por minuto, `phone:<hash>` para desconocidos 10 por hora y 30 por día. El número va con hash, nunca en claro.
- En memoria se queda para las pruebas y una instancia local.

## Fase 3: portal (srtomy-ui)

- `app/portal/[nodeId]/billing/page.tsx`: plan actual, uso (personas, sedes, mensajes de IA) con barras, funciones incluidas, precio para sus personas en Perú y Colombia, botones «Pagar 1 mes» y «Pagar 1 año» (`POST /billing/checkout` y redirige a `checkoutUrl`), historial de pagos, y al volver con `?pago=completado` llama `POST /billing/sync`.
- Un manejador de 402 en `app/(api)/client.ts` que muestra un aviso con «Ver planes» hacia esa pantalla, usando `code`.
- Avisos previos: en «Personas» al 80 % del máximo; en «WhatsApp», los días que quedan de prueba (`whatsappTrialEndsAt`).
- Landing (`app/landing/content.ts`, `Pricing.tsx`): los dos planes reales y una calculadora de precio por personas y país.
- Quitar Culqi: `types/culqi.d.ts`, el `Script` de `app/(components)/ProfileForm.tsx`, `NEXT_PUBLIC_CULQI_API_KEY` en `app/env.ts`, `.env.example` y el README. Actualizar la fila 27 de `docs/tech-debt-and-future-work.md`.
- Etiqueta en español del permiso nuevo `billing.manage` («Gestionar el plan y los pagos») en `app/(components)/permissionLabels.ts`.
- Pruebas con Vitest y Testing Library (`user-event`), con `@/app/(api)/generated` simulado.

## Fase 4: infraestructura y avisos

- Terraform (`infra/terraform/modules/db-secrets`): secretos `dlocalgo-api-key` y `dlocalgo-secret-key`; variables `DLOCALGO_URL` y `DLOCALGO_NOTIFICATION_URL` en los workflows de despliegue. En dev, sandbox.
- Recordatorio de vencimiento: un job de Cloud Scheduler diario (`POST /v1/internal/billing/remind`, job `billing-renewal-reminders`) que encola por el outbox un correo 7 días y 1 día antes de `paidUntil` y al entrar en gracia, a quien tiene `billing.manage`. Nunca `@Scheduled`. **Hecho**: `command/remind_renewals`, `command/RenewalReminderSender` (una transacción por iglesia) y `billing_reminder` (`V1.34.0`) para no repetir un aviso.
- Ritmo por IP en los endpoints públicos de la API: Bucket4j con su almacén de Postgres, por IP (`X-Forwarded-For` de Cloud Run), 30 por minuto en inscripción pública, QR y libro de reclamaciones. Se descarta Cloud Armor por ahora: exige un balanceador (unos US$ 18 al mes). **Hecho**: `shared/PublicRateLimitFilter` y `RateLimitConfig` (`rate-limit.*`), tabla `rate_limit_bucket` (`V1.35.0`). Se toma la **última** dirección de `X-Forwarded-For`, la que agrega Google; las anteriores las elige quien llama.
- Probar en el sandbox de dLocal Go un pago de Perú (tarjeta y Yape) y uno de Colombia (PSE).

## Riesgos

- **El precio y la cuota no están validados** (decisiones abiertas de la spec). Todo está en configuración para cambiarlo sin desplegar código.
- **Comprobantes electrónicos** (SUNAT y DIAN) no están resueltos: hay que decidirlos antes del primer cobro real.
- Las tarifas de dLocal Go son de su lista de 2024; confirmarlas al abrir la cuenta.
