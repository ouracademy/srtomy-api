package org.ouracademy.ovejafiel.organization.query;

import java.time.LocalDate;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;

/**
 * The person-specific fields of a party that turned out to be a person, as resolved through the
 * person module's query handlers (organization cannot read person data itself).
 */
public record PersonDetails(
    String firstName,
    String lastName,
    String phoneNumber,
    Integer age,
    LocalDate birthdate,
    Boolean hasBeenBaptized) {

  public static PersonDetails from(FindPersonResult person) {
    return new PersonDetails(
        person.firstName(),
        person.lastName(),
        person.phoneNumber(),
        person.age(),
        person.birthdate(),
        person.hasBeenBaptized());
  }
}
