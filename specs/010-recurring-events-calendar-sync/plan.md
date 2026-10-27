# Plan: Eventos recurrentes y calendario suscribible

Ver las decisiones en [`spec.md`](./spec.md).

## Modelo (`V1.8.0__recurring_events.sql`)

- Vacía los eventos y sus tablas dependientes (no hay datos en producción) y quita `event_type_id`, `days_of_week`, `recurrent_event_id`, `end_recur` de `event`.
- `event_series` (regla, zona, `materialized_until`, `fully_materialized`), `event_series_speakers`, `event.series_id`.
- `calendar_feed (user_id, token único)`.

## Código (módulo `event`)

- `domain`: `Recurrence` (valor embebido y expansión), `EventSeries`, `EventSeriesId`, `CalendarFeed`; `Event` pasa a una sola clase.
- `command`: `create_event` (serie), `delete_event` (por opción), `extend_event_series`, `ensure_calendar_feed`, `renew_calendar_feed`; `job.ExtendEventSeriesJob`.
- `query`: `get_calendar_feed` (`IcsCalendar`), `RecurrenceResult` en `FindEventResult` y `GetSharedEventResult`.
- `controller`: `CalendarFeedController`, `RecurrenceRequest`/`RecurrenceResponse`.

## Pruebas

`RecurrenceTests`, `EventSeriesTests`, `IcsCalendarTests`, pruebas de cada handler y `RecurringEventsIntegrationTests` (esquema real, serie, borrado, calendario y renovación).
