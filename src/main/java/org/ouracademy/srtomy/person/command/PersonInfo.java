package org.ouracademy.srtomy.person.command;

import java.time.LocalDate;
import org.ouracademy.srtomy.person.domain.Person;

/**
 * The editable personal data shared by {@link CreatePersonCommand} and {@link UpdatePersonCommand}.
 */
public record PersonInfo(
    String firstName,
    String lastName,
    String phoneNumber,
    Integer age,
    LocalDate birthdate,
    Boolean hasBeenBaptized) {

  void applyTo(Person person) {
    person.setFirstName(firstName);
    person.setLastName(lastName);
    person.setName(firstName + " " + lastName);
    person.setPhoneNumber(phoneNumber);
    person.setAge(age);
    person.setBirthdate(birthdate);
    person.setHasBeenBaptized(hasBeenBaptized);
  }
}
