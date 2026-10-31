package org.ouracademy.ovejafiel.person.command;

import java.time.LocalDate;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommand;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonCommand;
import org.ouracademy.ovejafiel.person.domain.Person;

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
    Boolean hasBeenBaptized,
    Boolean whatsappConsent) {

  /** Without a WhatsApp choice: the person's current one is kept. */
  public PersonInfo(
      String firstName,
      String lastName,
      String phoneNumber,
      String email,
      Integer age,
      LocalDate birthdate,
      Boolean hasBeenBaptized) {
    this(firstName, lastName, phoneNumber, email, age, birthdate, hasBeenBaptized, null);
  }

  public void applyTo(Person person) {
    person.setFirstName(firstName);
    person.setLastName(lastName);
    person.setName((firstName + " " + (lastName == null ? "" : lastName)).trim());
    person.setPhoneNumber(phoneNumber);
    person.setEmail(email == null || email.isBlank() ? null : email.trim());
    person.setAge(age);
    person.setBirthdate(birthdate);
    person.setHasBeenBaptized(hasBeenBaptized);
    if (whatsappConsent != null) {
      person.setWhatsappConsent(whatsappConsent);
    }
  }
}
