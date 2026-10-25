package org.ouracademy.srtomy.event;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.EventSeries;
import org.ouracademy.srtomy.event.domain.Recurrence;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.person.domain.Person;
import org.springframework.test.util.ReflectionTestUtils;

/** Test helpers for the event module's tests. */
public final class EventFixtures {

  public static final ZonedDateTime START = ZonedDateTime.parse("2026-01-05T15:00:00Z[UTC]");
  public static final ZonedDateTime END = ZonedDateTime.parse("2026-01-05T17:00:00Z[UTC]");

  private EventFixtures() {}

  public static Event singleEvent(
      EventId id, String title, Organization organization, List<Person> speakers) {
    var event = new Event(title, START, END);
    ReflectionTestUtils.setField(event, "id", id);
    event.setOrganization(organization);
    event.setSpeakers(speakers);
    event.setAddress("Av. Siempre Viva 123");
    event.setDescription("Culto dominical");
    event.setIsACourse(false);
    return event;
  }

  /** Mondays and Wednesdays, 3pm to 5pm UTC, from 2026-01-05 until 2026-01-14. */
  public static EventSeries eventSeries(Organization organization) {
    var series =
        new EventSeries(
            "Servicio",
            LocalDateTime.of(2026, 1, 5, 15, 0),
            Duration.ofHours(2),
            ZoneId.of("UTC"),
            Recurrence.of(
                Recurrence.Frequency.WEEKLY,
                1,
                List.of(1, 3),
                null,
                Recurrence.EndType.UNTIL,
                LocalDate.of(2026, 1, 14),
                null));
    series.setOrganization(organization);
    series.setSpeakers(List.of());
    return series;
  }

  public static Event occurrenceOf(EventSeries series, EventId id) {
    var event = singleEvent(id, series.getTitle(), series.getOrganization(), List.of());
    ReflectionTestUtils.setField(event, "series", series);
    return event;
  }
}
