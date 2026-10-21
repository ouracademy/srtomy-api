package org.ouracademy.srtomy.event.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.ouracademy.srtomy.event.query.FindEventResult;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.person.query.FindPersonResult;

/** Same JSON fields the endpoints returned when they serialized the SingleEvent entity. */
public record EventResponse(
    UUID id,
    String title,
    List<SpeakerResponse> speakers,
    ZonedDateTime start,
    ZonedDateTime end,
    String address,
    OrganizationResponse organization,
    String description,
    Boolean isACourse,
    RecurrentEventResponse recurrentEvent,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {

  public static EventResponse from(FindEventResult result) {
    var recurrentEvent = result.recurrentEvent();
    return new EventResponse(
        result.id().value(),
        result.title(),
        speakersOf(result.speakers()),
        result.start(),
        result.end(),
        result.address(),
        OrganizationResponse.from(result.organization()),
        result.description(),
        result.isACourse(),
        recurrentEvent == null ? null : RecurrentEventResponse.from(recurrentEvent),
        result.createDate(),
        result.lastModifiedDate());
  }

  public record RecurrentEventResponse(
      UUID id,
      String title,
      List<SpeakerResponse> speakers,
      ZonedDateTime start,
      ZonedDateTime end,
      String address,
      OrganizationResponse organization,
      String description,
      Boolean isACourse,
      LocalDate endRecur,
      List<Integer> daysOfWeek,
      LocalDateTime createDate,
      LocalDateTime lastModifiedDate) {

    static RecurrentEventResponse from(FindEventResult.RecurrentEventResult result) {
      return new RecurrentEventResponse(
          result.id().value(),
          result.title(),
          speakersOf(result.speakers()),
          result.start(),
          result.end(),
          result.address(),
          OrganizationResponse.from(result.organization()),
          result.description(),
          result.isACourse(),
          result.endRecur(),
          result.daysOfWeek(),
          result.createDate(),
          result.lastModifiedDate());
    }
  }

  /** Same JSON fields as the serialized Person entity. */
  public record SpeakerResponse(
      UUID id,
      String name,
      String fullName,
      String firstName,
      String lastName,
      String description,
      String phoneNumber,
      String address,
      Integer age,
      LocalDate birthdate,
      Boolean hasBeenBaptized,
      LocalDateTime createDate,
      LocalDateTime lastModifiedDate) {

    static SpeakerResponse from(FindPersonResult result) {
      return new SpeakerResponse(
          result.id().value(),
          result.name(),
          result.name(),
          result.firstName(),
          result.lastName(),
          result.description(),
          result.phoneNumber(),
          result.address(),
          result.age(),
          result.birthdate(),
          result.hasBeenBaptized(),
          result.createDate(),
          result.lastModifiedDate());
    }
  }

  /**
   * Same JSON fields as the serialized Organization entity, including {@code deletable} (its {@code
   * isDeletable()} getter).
   */
  public record OrganizationResponse(
      UUID id,
      String name,
      String description,
      String phoneNumber,
      String address,
      Organization.Type type,
      boolean deletable,
      LocalDateTime createDate,
      LocalDateTime lastModifiedDate) {

    static OrganizationResponse from(FindOrganizationResult result) {
      if (result == null) {
        return null;
      }
      return new OrganizationResponse(
          result.id().value(),
          result.name(),
          result.description(),
          result.phoneNumber(),
          result.address(),
          result.type(),
          result.type() == Organization.Type.GROUP || result.type() == Organization.Type.MINISTRY,
          result.createDate(),
          result.lastModifiedDate());
    }
  }

  private static List<SpeakerResponse> speakersOf(List<FindPersonResult> speakers) {
    return speakers.stream().map(SpeakerResponse::from).toList();
  }
}
