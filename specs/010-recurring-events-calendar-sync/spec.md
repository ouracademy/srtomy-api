# Feature Specification: Eventos recurrentes y calendario suscribible

**Feature Branch**: `claude/recurring-events-calendar-sync-mmefu3`

**Created**: 2026-10-25

**Status**: Implemented (API); la UI va en el repo `srtomy-ui`

**Input**: Los eventos recurrentes de hoy solo repiten por días de la semana y **siempre exigen fecha de fin**. Se quiere repetir como en Google Calendar (cada N días, semanas, meses o años; terminando nunca, en una fecha o tras N veces) y que cada usuario pueda **ver sus eventos en Google Calendar o Apple Calendar** suscribiéndose a un enlace privado. Aún no hay datos en producción, así que el modelo se reemplaza sin compatibilidad hacia atrás.

## Decisiones

1. **Una serie y sus ocurrencias**: `EventSeries` guarda la regla (frecuencia, intervalo, días de la semana, modo mensual, fin, zona horaria) y `Event` es una ocurrencia (`series_id`, nulo en un evento suelto). Asistencia, inscripciones y compartir siguen colgando de cada `event_id`, sin cambios. Se elimina la herencia `SingleEvent`/`RecurrentEvent`, `days_of_week`, `end_recur` y los indicadores `recurrent`/`validDaysOfWeek` de la API.
2. **Regla estructurada, sin librería**: la regla se guarda en columnas (no como texto RRULE) y se expande con `java.time` (`Recurrence.expand`). No se agrega ical4j (principio V de la constitución): el modelo es pequeño, la UI lo edita campo por campo y el calendario se escribe con un serializador propio de RFC 5545 (`IcsCalendar`). `Recurrence.toRRule` queda como descripción informativa.
3. **Hora local estable**: las ocurrencias se calculan en la zona de la serie (IANA, ej. `America/Lima`) y se guardan en UTC; con el horario de verano la hora local se mantiene. Un `timeZone` ausente significa UTC.
4. **Fin**: `NEVER`, `UNTIL` (fecha incluida) o `COUNT` (1 a 1000). Intervalo de 1 a 99. Semana que empieza el lunes; semanal sin días repite el día de la primera ocurrencia; mensual puede ser «el día N» (los meses sin ese día se saltan) o «el n-ésimo día de la semana» (el quinto es el último).
5. **Horizonte móvil**: al crear se materializan 12 meses de ocurrencias (`materialized_until`); una serie sin fin (o con fin lejano) se extiende con `ExtendEventSeriesJob` (diario, 03:30 UTC) que añade las faltantes cuando quedan menos de 6 meses. Dos instancias no duplican: la consulta bloquea las series (`PESSIMISTIC_WRITE`).
6. **Borrar**: `THIS_EVENT` borra una ocurrencia; `THIS_AND_THE_FOLLOWING_EVENTS` borra desde esa y termina la serie el día anterior (si no queda ningún día, borra la serie); `ALL_EVENTS` borra todas y la serie.
7. **Calendario privado**: cada usuario tiene un token (`calendar_feed`, 96 bits como `share_slug`) y la dirección pública `GET /v1/public/calendar/{token}.ics`. Las apps de calendario no pueden enviar cabeceras, así que el token en la URL es la credencial; `POST /v1/me/calendar-feed/renew` genera otro y revoca el anterior. Token desconocido o renovado: 404.
8. **Qué incluye el calendario**: los eventos de los nodos donde el usuario tiene `events.read` (incluido el nivel Miembro de la 008) y de todo lo que cuelga de ellos, de 30 días atrás a 366 días adelante (máximo 5000). Un `VEVENT` por ocurrencia (así borrar una ocurrencia no necesita `EXDATE`). Solo lectura: Google Calendar refresca cada 12–24 h, Apple Calendar respeta `REFRESH-INTERVAL`.
9. **Fuera de alcance**: sincronización bidireccional con Google (OAuth) y edición de eventos (deuda 16).

## User Scenarios & Testing

### US1 — Repetir como en Google Calendar (P1)

1. **Given** un evento semanal los lunes y miércoles que termina tras 5 veces, **When** se crea, **Then** hay 5 ocurrencias, todas con la misma `recurrence`.
2. **Given** una repetición diaria que no termina, **When** se crea, **Then** hay un año de ocurrencias y el job agrega más con el tiempo.
3. **Given** una repetición «hasta una fecha» sin fecha, **Then** 400.
4. **Given** una serie, **When** se borra «este y los siguientes» desde la tercera ocurrencia, **Then** quedan las dos primeras.

### US2 — Ver mis eventos en Google o Apple Calendar (P1)

1. **Given** un usuario con acceso de lectura a eventos, **When** pide `GET /me/calendar-feed`, **Then** recibe su dirección `.ics`; pedirla otra vez da la misma.
2. **Given** esa dirección, **When** una app de calendario la lee sin sesión, **Then** recibe `text/calendar` con un `VEVENT` por ocurrencia.
3. **Given** un usuario sin acceso a ningún nodo, **Then** su calendario está vacío.
4. **Given** que renueva la dirección, **Then** la vieja devuelve 404 y la nueva funciona.

## Requirements

- **FR-001**: `POST /events` acepta `recurrence {frequency, interval, daysOfWeek, monthlyMode, endType, until, count}` y `timeZone`; sin `recurrence` crea un evento suelto.
- **FR-002**: `EventResponse` y la página compartida devuelven `recurrence {seriesId, …, timeZone}` (nulo en un evento suelto).
- **FR-003**: `GET /me/calendar-feed` y `POST /me/calendar-feed/renew` (con sesión) devuelven `{path}`; `GET /public/calendar/{token}.ics` no pide sesión.
- **FR-004**: el calendario no incluye datos de asistentes ni de inscritos.
