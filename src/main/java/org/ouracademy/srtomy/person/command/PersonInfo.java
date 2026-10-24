package org.ouracademy.srtomy.person.command;

import java.time.LocalDate;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonCommand;
import org.ouracademy.srtomy.person.command.update_person.UpdatePersonCommand;
import org.ouracademy.srtomy.person.domain.Person;

/**
 * The editable personal data shared by {@link CreatePersonCommand} and {@link UpdatePersonCommand}.
 */
public record PersonInfo(
    String firstName,
    String lastName,
    String phoneNumber,
    String email,
    Integer age,
    LocalDate birthdate,
    Boolean hasBeenBaptized) {

  public void applyTo(Person person) {
    person.setFirstName(firstName);
    person.setLastName(lastName);
    person.setName(firstName + " " + lastName);
    person.setPhoneNumber(phoneNumber);
    person.setEmail(email == null || email.isBlank() ? null : email.trim());
    person.setAge(age);
    person.setBirthdate(birthdate);
    person.setHasBeenBaptized(hasBeenBaptized);
  }
}
