# 0001. Sin zona horaria por organización

- **Estado:** Aceptada (decidida por el dueño del producto, nov 2026).
- **Antes:** ítem 20 de la deuda técnica.

## Contexto

La zona horaria de una serie de eventos se toma del navegador de quien la crea (`EventSeries`, spec `010-recurring-events-calendar-sync`). No existe una zona guardada en la organización. El ítem proponía guardar la zona en la iglesia y usarla como valor por defecto al crear eventos.

## Decisión

No se agrega una zona horaria a la organización. Se sigue tomando la del navegador de quien crea la serie.

## Motivo

El dueño del producto indicó que no la necesita. No se registró un motivo más largo; si aparece uno (por ejemplo, iglesias con sedes en varias zonas), se reabre con un ADR nuevo.

## Consecuencias

- Sin columna nueva en `organization` ni campo en los formularios de iglesia o sede.
- Quien crea una serie desde un navegador con otra zona obtiene la hora de esa zona.
