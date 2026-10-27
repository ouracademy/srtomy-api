package org.ouracademy.ovejafiel.event.query;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;

/**
 * A single event as exposed outside the module (never the JPA entity), with the same data the
 * {@link Event} entity used to expose. {@code recurrence} is null for one-off events.
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
    RecurrenceResult recurrence,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {

  public static FindEventResult from(Event event) {
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
        RecurrenceResult.from(event.getSeries()),
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
