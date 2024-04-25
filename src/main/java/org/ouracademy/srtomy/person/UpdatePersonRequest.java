package org.ouracademy.srtomy.person;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    @NotEmpty
    Set<Long> roles = new HashSet<>();

    @NotNull
    Long organizationId;
}
