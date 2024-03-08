package org.ouracademy.srtomy.person;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class PersonRequest {

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

    @NotNull
    Long organizationId;
}
