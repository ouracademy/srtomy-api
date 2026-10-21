package org.ouracademy.srtomy.event;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.RecurrentEvent;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.person.domain.Person;
import org.springframework.test.util.ReflectionTestUtils;

/** Test helpers for the event module's tests. */
public final class EventFixtures {

  public static final ZonedDateTime START = ZonedDateTime.parse("2026-01-05T15:00:00Z[UTC]");
  public static final ZonedDateTime END = ZonedDateTime.parse("2026-01-05T17:00:00Z[UTC]");

  private EventFixtures() {}

  public static SingleEvent singleEvent(
      EventId id, String title, Organization organization, List<Person> speakers) {
    var event = new SingleEvent(title, START, END);
    ReflectionTestUtils.setField(event, "id", id);
    event.setOrganization(organization);
    event.setSpeakers(speakers);
    event.setAddress("Av. Siempre Viva 123");
    event.setDescription("Culto dominical");
    event.setIsACourse(false);
    return event;
  }

  public static RecurrentEvent recurrentEvent(EventId id, Organization organization) {
    var event =
        new RecurrentEvent("Servicio", START, END, List.of(1, 3), LocalDate.of(2026, 1, 14));
    ReflectionTestUtils.setField(event, "id", id);
    event.setOrganization(organization);
    event.setSpeakers(List.of());
    return event;
  }

  public static SingleEvent occurrenceOf(RecurrentEvent recurrentEvent, EventId id) {
    var event =
        singleEvent(id, recurrentEvent.getTitle(), recurrentEvent.getOrganization(), List.of());
    ReflectionTestUtils.setField(event, "recurrentEvent", recurrentEvent);
    return event;
  }
}
