package org.ouracademy.srtomy.person;

import java.time.LocalDate;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.person.query.FindPersonResult;
import org.springframework.test.util.ReflectionTestUtils;

/** Test helpers for the person module's tests (and for modules that consume person results). */
public final class PersonFixtures {

  private PersonFixtures() {}

  public static Person person(PartyId id, String firstName, String lastName) {
    var person = new Person();
    ReflectionTestUtils.setField(person, "id", id);
    person.setFirstName(firstName);
    person.setLastName(lastName);
    person.setName(firstName + " " + lastName);
    person.setPhoneNumber("999");
    person.setAge(30);
    person.setBirthdate(LocalDate.of(1995, 5, 17));
    person.setHasBeenBaptized(true);
    return person;
  }

  public static FindPersonResult personResult(PartyId id, String firstName, String lastName) {
    return FindPersonResult.from(person(id, firstName, lastName));
  }
}
