package org.ouracademy.ovejafiel.person.controller.dto;

import jakarta.validation.constraints.Email;
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
import org.ouracademy.ovejafiel.person.domain.MemberState;

@Getter
@Setter
public class CreatePersonRequest {

  @NotBlank String firstName;
  @NotBlank String lastName;

  // TODO: validate
  String phoneNumber;

  @Email String email;

  @PositiveOrZero Integer age;
  @PastOrPresent LocalDate birthdate;

  Boolean hasBeenBaptized;

  /**
   * How involved the person is: visitor, membership in progress or member. Optional (a new person
   * is a visitor); whoever holds a position is a member whatever it says.
   */
  MemberState memberState;

  /** Ids of positions (cargos) of the church; others are ignored. */
  Set<UUID> positionIds = new HashSet<>();

  @NotNull UUID organizationId;
}
