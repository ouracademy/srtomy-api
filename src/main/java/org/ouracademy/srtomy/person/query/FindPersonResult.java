package org.ouracademy.srtomy.person.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;

/** A person as exposed outside the module (never the JPA entity). {@code name} is the full name. */
public record FindPersonResult(
    PartyId id,
    String name,
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

  public static FindPersonResult from(Person person) {
    return new FindPersonResult(
        person.getId(),
        person.getName(),
        person.getFirstName(),
        person.getLastName(),
        person.getDescription(),
        person.getPhoneNumber(),
        person.getEmail(),
        person.getAddress(),
        person.getAge(),
        person.getBirthdate(),
        person.getHasBeenBaptized(),
        person.getCreateDate(),
        person.getLastModifiedDate());
  }
}
