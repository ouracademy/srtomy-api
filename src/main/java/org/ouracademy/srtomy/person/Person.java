package org.ouracademy.srtomy.person;

import java.time.LocalDate;

import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Person extends DomainObject {

    @NotBlank
    String firstName;
    @NotBlank
    String lastName;

    // TODO: validate
    String phoneNumber;

    @PositiveOrZero
    Integer age;
    @PastOrPresent
    LocalDate birthdate;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
