package org.ouracademy.ovejafiel.person.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
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
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {

  public static PersonResponse from(FindPersonResult result) {
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
        result.createDate(),
        result.lastModifiedDate());
  }
}
