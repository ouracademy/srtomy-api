package org.ouracademy.ovejafiel.organization.controller.dto;

import java.time.LocalDate;
import java.util.UUID;
import org.ouracademy.ovejafiel.organization.query.PersonDetails;
import org.ouracademy.ovejafiel.organization.query.get_location_children.GetLocationChildrenResult;

/**
 * A member or leader of a group/ministry. The person fields sit at the top level, as when the
 * endpoint serialized the Person entity; they are null when the id does not resolve to a person.
 */
public record LocationChildMemberResponse(
    UUID id,
    String name,
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
          child.id().value(), child.name(), null, null, null, null, null, null, null);
    }
    return new LocationChildMemberResponse(
        child.id().value(),
        child.name(),
        child.name(),
        person.firstName(),
        person.lastName(),
        person.phoneNumber(),
        person.age(),
        person.birthdate(),
        person.hasBeenBaptized());
  }
}
