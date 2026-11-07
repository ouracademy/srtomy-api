package org.ouracademy.ovejafiel.event.query;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
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
    LocalDateTime lastModifiedDate,
    EventId parentEventId,
    String parentTitle,
    RegistrationMode registrationMode,
    String track,
    List<ChildResult> children,
    int programSize) {

  /** A sub-event listed inside its container. */
  public record ChildResult(
      EventId id,
      String title,
      ZonedDateTime start,
      ZonedDateTime end,
      Boolean isACourse,
      RegistrationMode registrationMode,
      String track) {

    static ChildResult from(Event child) {
      return new ChildResult(
          child.getId(),
          child.getTitle(),
          child.getStart(),
          child.getEnd(),
          child.getIsACourse(),
          child.getRegistrationMode(),
          child.getTrack());
    }
  }

  /** The same event saying how many sub-events it has, when the list does not carry them. */
  public FindEventResult withProgramSize(int size) {
    return new FindEventResult(
        id,
        title,
        speakers,
        start,
        end,
        address,
        organization,
        description,
        isACourse,
        recurrence,
        createDate,
        lastModifiedDate,
        parentEventId,
        parentTitle,
        registrationMode,
        track,
        children,
        size);
  }

  public static FindEventResult from(Event event) {
    return from(event, List.of());
  }

  /** {@code children} are the sub-events of {@code event}, in chronological order. */
  public static FindEventResult from(Event event, List<Event> children) {
    var parent = event.getParent();
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
        event.getLastModifiedDate(),
        parent == null ? null : parent.getId(),
        parent == null ? null : parent.getTitle(),
        event.getRegistrationMode(),
        event.getTrack(),
        children.stream().map(ChildResult::from).toList(),
        children.size());
  }

  private static List<FindPersonResult> speakersOf(Event event) {
    return event.getSpeakers().stream().map(FindPersonResult::from).toList();
  }

  private static FindOrganizationResult organizationOf(Event event) {
    var organization = event.getOrganization();
    return organization == null ? null : FindOrganizationResult.from(organization);
  }
}
