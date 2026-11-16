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
| 401 | Una notificación de pago sin firma válida | — |
