package org.ouracademy.ovejafiel.person.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;

/** Same JSON fields the endpoints returned when they serialized the Person entity. */
public record PersonResponse(
    UUID id,
    String name,
    String fullName,
    String firstName,
    String lastName,
    String description,
    String phoneNumber,
    String email,
    String address,
    Integer age,
    LocalDate birthdate,
    Boolean hasBeenBaptized,
    boolean whatsappConsent,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate,
    MemberState memberState) {

  /** The person alone, without how they stand in any node. */
  public static PersonResponse from(FindPersonResult result) {
    return from(result, null);
  }

  /** The person and, when given, their state in the node that was listed. */
  public static PersonResponse from(FindPersonResult result, MemberState state) {
    return new PersonResponse(
        result.id().value(),
        result.name(),
        result.name(),
        result.firstName(),
        result.lastName(),
        result.description(),
        result.phoneNumber(),
        result.email(),
        result.address(),
        result.age(),
        result.birthdate(),
        result.hasBeenBaptized(),
        result.whatsappConsent(),
        result.createDate(),
        result.lastModifiedDate(),
        state);
  }
}
