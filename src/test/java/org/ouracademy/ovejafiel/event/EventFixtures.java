package org.ouracademy.ovejafiel.event;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.event.domain.Recurrence;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.TicketType;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.person.domain.Person;
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

  /** A one-off event at the given times (tests about "now" need an event around now). */
  public static Event eventBetween(
      EventId id, String title, Organization organization, ZonedDateTime start, ZonedDateTime end) {
    var event = new Event(title, start, end);
    ReflectionTestUtils.setField(event, "id", id);
    event.setOrganization(organization);
    event.setAddress("Av. Siempre Viva 123");
    event.setIsACourse(false);
    return event;
  }

  /** An event with an open registration; call {@code configure} again to change it. */
  public static EventRegistrationSetup registrationSetup(Event event) {
    var setup = new EventRegistrationSetup(event);
    setup.configure(false);
    return setup;
  }

  public static TicketType ticket(Event event, String name, Integer quantity) {
    var ticket = new TicketType(event, 0);
    ticket.describe(name, 0, "PEN", quantity, null, null);
    return ticket;
  }

  public static EventRegistration registration(
      Event event, TicketType ticket, String email, RegistrationStatus status) {
    return new EventRegistration(event, ticket, null, "Lu", "Mora", email, null, status);
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
