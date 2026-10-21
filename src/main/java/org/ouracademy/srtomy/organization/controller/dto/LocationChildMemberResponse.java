package org.ouracademy.srtomy.organization.controller.dto;

import java.time.LocalDate;
import java.util.UUID;
import org.ouracademy.srtomy.organization.query.GetLocationChildrenResult;
import org.ouracademy.srtomy.organization.query.PersonDetails;

/**
 * A member or leader of a group/ministry; {@code kind} tells organizations and people apart. For
 * people, the person fields sit at the top level, as when the endpoint serialized the Person
 * entity; they are null for organizations.
 */
public record LocationChildMemberResponse(
    UUID id,
    String name,
    GetLocationChildrenResult.Kind kind,
    String fullName,
    String firstName,
    String lastName,
    String phoneNumber,
    Integer age,
    LocalDate birthdate,
    Boolean hasBeenBaptized) {

  public static LocationChildMemberResponse from(GetLocationChildrenResult.Child child) {
    PersonDetails person = child.person();
    if (person == null) {
      return new LocationChildMemberResponse(
          child.id().value(), child.name(), child.kind(), null, null, null, null, null, null, null);
    }
    return new LocationChildMemberResponse(
        child.id().value(),
        child.name(),
        child.kind(),
        child.name(),
        person.firstName(),
        person.lastName(),
        person.phoneNumber(),
        person.age(),
        person.birthdate(),
        person.hasBeenBaptized());
  }
}
