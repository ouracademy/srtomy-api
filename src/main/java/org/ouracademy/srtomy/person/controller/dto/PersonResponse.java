package org.ouracademy.srtomy.person.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.srtomy.person.query.FindPersonResult;

/** Same JSON fields the endpoints returned when they serialized the Person entity. */
public record PersonResponse(
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
    LocalDateTime lastModifiedDate,
    String organizationName,
    Boolean directMember) {

  public static PersonResponse from(FindPersonResult result) {
    return from(result, null, null);
  }

  /**
   * For listings that include sub-nodes: where the person comes from and if it is the node itself.
   */
  public static PersonResponse from(
      FindPersonResult result, String organizationName, Boolean directMember) {
    return new PersonResponse(
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
        result.lastModifiedDate(),
        organizationName,
        directMember);
  }
}
