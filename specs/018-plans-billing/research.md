# Research: pasarela de pagos para Perú y Colombia

Fecha: 2026-11-15. Las tarifas salen de listas de precios públicas y de blogs comparativos; **confírmalas con cada proveedor antes de firmar**.

## Qué necesitamos

- Cobrar a iglesias de **Perú y Colombia** en su moneda (PEN y COP), con tarjeta y con los medios que usa la gente: Yape y Plin en Perú, PSE y Nequi en Colombia.
- Montos que **cambian con las personas** de la iglesia.
- Una sola integración y, si se puede, una sola cuenta.
- La empresa está en Perú.

## Opciones

| Opción | Perú | Colombia | Una cuenta para ambos | Cobro recurrente | Notas |
| --- | --- | --- | --- | --- | --- |
| **Culqi** (lo de hoy) | Sí (tarjeta, Yape) | No | No | Básico | Solo Perú; el portal lo carga sin usarlo. |
| **Mercado Pago** | Sí | Sí | No: hay una cuenta por país | Sí (`/preapproval`) | Suscripciones con monto fijo; habría que integrar dos cuentas. |
| **Stripe** | No como empresa peruana | Sí | No | Muy bueno | Exige una empresa en un país soportado (por ejemplo una LLC en EE. UU.). |
| **Rebill** | Sí | Sí | Sí | Es su fuerte | 4,0–4,3 % + USD 0,20 por cobro con tarjeta; confirmar si una empresa peruana cobra en Colombia sin entidad allá. |
| **dLocal Go** | Sí (tarjeta, Yape) | Sí (tarjeta, PSE) | **Sí** | Sí (planes de monto fijo) | Sin costo fijo; ~2,99 % en Perú y ~1,99 % + USD 0,20 en Colombia según su lista de 2024, más impuestos locales. |
| **Paddle / Lemon Squeezy** (vendedor de registro) | Vende a Perú | Vende a Colombia | Sí | Sí | Emiten el comprobante y pagan impuestos por ti, pero cobran ~5 % + fijo, cobran en USD y no ofrecen Yape ni PSE. No confirmamos pagos a una empresa peruana. |

## Decisión

**dLocal Go, con pagos únicos (prepago mensual o anual).**

- Una sola cuenta y una sola API cobran en soles y en pesos, con tarjeta, Yape y PSE.
- Sin costo fijo mensual: cuadra con un producto que recién empieza a cobrar.
- **Prepago y no débito automático** porque Yape y PSE no se pueden debitar solos, y porque el monto cambia con las personas (los planes de suscripción de dLocal Go son de monto fijo). Avisar antes del vencimiento cubre la renovación.
- Más adelante, para quien pague con tarjeta, se pueden sumar las suscripciones de dLocal Go (`POST /v1/subscription/plan`) sin tocar el resto.

Si el dueño prefiere no lidiar con comprobantes ni impuestos de dos países, la alternativa es un vendedor de registro (Paddle), a cambio de cobrar en dólares y perder Yape y PSE.

## Cómo funciona dLocal Go (lo que usa el código)

- Base: `https://api.dlocalgo.com` (sandbox `https://api-sbx.dlocalgo.com`). Autenticación: `Authorization: Bearer <api_key>:<secret_key>`.
- `POST /v1/payments` con `amount`, `currency`, `country`, `order_id` (nuestro id de factura), `success_url`, `back_url` y `notification_url` → responde `id` y `redirect_url` (el checkout).
- `GET /v1/payments/{id}` → `status`: `PENDING`, `PAID`, `REJECTED`, `CANCELLED` o `EXPIRED`, y el `order_id`.
- Notificación: `POST` a `notification_url` con `{"payment_id": "..."}` y el encabezado `Authorization: V2-HMAC-SHA256, Signature: <hex>`, donde la firma es HMAC-SHA256 de `api_key + cuerpo` con la `secret_key`. Reintenta cada 10 minutos hasta 30 días mientras no respondamos 200.

## Costo del asistente de IA (para las cuotas)

Precios de la API de Anthropic (noviembre de 2026), por millón de tokens:

| Modelo | Entrada | Salida | Lectura de caché |
| --- | --- | --- | --- |
| Claude Sonnet 5.5 (el del agente hoy) | US$ 2,00 | US$ 10,00 | US$ 0,10 |
| Claude Haiku 5.5 | US$ 0,10 | US$ 0,50 | — |

Estimación por mensaje respondido con Sonnet 5.5, de 2 a 3 pasos del modelo con unos 20.000 tokens de instrucciones y herramientas (casi todo en caché), 2.000 tokens nuevos y 800 de salida por paso: **entre US$ 0,02 y 0,05**. Es una estimación: el valor real sale de las trazas de LangSmith, que ya llevan la iglesia y los tokens.

## Fuentes

- [dLocal Go: crear un pago](https://docs.dlocalgo.com/integration-api/welcome-to-dlocal-go-api/payments/create-a-payment), [consultar un pago](https://docs.dlocalgo.com/integration-api/welcome-to-dlocal-go-api/payments/retrieve-a-payment), [notificaciones](https://docs.dlocalgo.com/integration-api/welcome-to-dlocal-go-api/payments/notifications), [suscripciones](https://docs.dlocalgo.com/integration-api/welcome-to-dlocal-go-api/subscriptions), [cobertura](https://dlocalgo.com/en/coverage), [lista de precios](https://dlocalgo.com/wp-content/uploads/2024/09/pricinglist.pdf)
- [dLocal: Perú (Yape recurrente)](https://docs.dlocal.com/docs/peru)
- [Mercado Pago Perú: suscripciones](https://www.mercadopago.com.pe/developers/es/reference/online-payments/subscriptions/create-preapproval-plan/post) y [Colombia](https://www.mercadopago.com.co/developers/es/reference/online-payments/subscriptions/get-preapproval/get)
- [Rebill: precios](https://www.rebill.com/en/pricing) y [Rebill frente a dLocal y EBANX](https://www.rebill.com/blog/rebill-vs-dlocal-vs-ebanx-latin-america)
- [Cómo abrir Stripe desde Perú](https://www.zenind.com/help/post/how-to-open-a-stripe-account-in-peru-a-practical-guide-for-founders-using-a-us-company)
- [Paddle: países soportados](https://developer.paddle.com/concepts/sell/supported-countries-locales)
- [Pasarelas de pago en Perú 2026 (Riqra)](https://blog.riqra.com/posts/pasarelas-pago-online-peru), [Pasarelas en Colombia 2026 (OnePay)](https://www.onepay.la/pasarelas-de-pago-colombia), [Pasarelas en LATAM 2026](https://cristiantala.com/pasarelas-de-pago-en-latam-2026-la-guia-que-necesitas-antes-de-cobrar-tu-primer-dolar/)
