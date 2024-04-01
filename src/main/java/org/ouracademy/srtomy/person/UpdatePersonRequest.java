package org.ouracademy.srtomy.person;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePersonRequest {

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

    Boolean hasBeenBaptized;
}
