package org.ouracademy.srtomy.person.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePersonRequest {

  @NotBlank String firstName;
  @NotBlank String lastName;

  // TODO: validate
  String phoneNumber;

  @PositiveOrZero Integer age;
  @PastOrPresent LocalDate birthdate;

  Boolean hasBeenBaptized;

  Set<UUID> roles = new HashSet<>();

  @NotNull UUID organizationId;
}
