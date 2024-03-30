package org.ouracademy.srtomy.person;

import java.time.LocalDate;

import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Person extends DomainObject {

    String firstName;
    String lastName;

    // TODO: validate
    String phoneNumber;
    Integer age;
    LocalDate birthdate;
    Boolean hasBeenBaptized;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
