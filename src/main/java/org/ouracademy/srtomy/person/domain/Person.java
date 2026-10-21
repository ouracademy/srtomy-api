package org.ouracademy.srtomy.person.domain;

import jakarta.persistence.Entity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.srtomy.party.domain.Party;

@Entity
@Getter
@Setter
public class Person extends Party {

  String firstName;
  String lastName;

  Integer age;
  LocalDate birthdate;
  Boolean hasBeenBaptized;

  public String getFullName() {
    return name;
  }
}
