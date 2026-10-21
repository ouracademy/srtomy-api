package org.ouracademy.srtomy.event.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.RecurrentEvent;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.person.query.FindPersonResult;

/**
 * A single event as exposed outside the module (never the JPA entity), with the same data the
 * {@link SingleEvent} entity used to expose. {@code recurrentEvent} is null for one-off events.
 */
public record FindEventResult(
    EventId id,
    String title,
    List<FindPersonResult> speakers,
    ZonedDateTime start,
    ZonedDateTime end,
    String address,
    FindOrganizationResult organization,
    String description,
    Boolean isACourse,
    RecurrentEventResult recurrentEvent,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {

  /** The recurrence a single event was expanded from. */
  public record RecurrentEventResult(
      EventId id,
      String title,
      List<FindPersonResult> speakers,
      ZonedDateTime start,
      ZonedDateTime end,
      String address,
      FindOrganizationResult organization,
      String description,
      Boolean isACourse,
      LocalDate endRecur,
      List<Integer> daysOfWeek,
      LocalDateTime createDate,
      LocalDateTime lastModifiedDate) {

    static RecurrentEventResult from(RecurrentEvent event) {
      return new RecurrentEventResult(
          event.getId(),
          event.getTitle(),
          speakersOf(event),
          event.getStart(),
          event.getEnd(),
          event.getAddress(),
          organizationOf(event),
          event.getDescription(),
          event.getIsACourse(),
          event.getEndRecur(),
          event.getDaysOfWeek() == null ? null : List.copyOf(event.getDaysOfWeek()),
          event.getCreateDate(),
          event.getLastModifiedDate());
    }
  }

  public static FindEventResult from(SingleEvent event) {
    var recurrentEvent = event.getRecurrentEvent();
    return new FindEventResult(
        event.getId(),
        event.getTitle(),
        speakersOf(event),
        event.getStart(),
        event.getEnd(),
        event.getAddress(),
        organizationOf(event),
        event.getDescription(),
        event.getIsACourse(),
        recurrentEvent == null ? null : RecurrentEventResult.from(recurrentEvent),
        event.getCreateDate(),
        event.getLastModifiedDate());
  }

  private static List<FindPersonResult> speakersOf(Event event) {
    return event.getSpeakers().stream().map(FindPersonResult::from).toList();
  }

  private static FindOrganizationResult organizationOf(Event event) {
    var organization = event.getOrganization();
    return organization == null ? null : FindOrganizationResult.from(organization);
  }
}
