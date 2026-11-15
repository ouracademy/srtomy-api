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

  /** First names. */
  @NotBlank String firstName;

  /** Last names. */
  @NotBlank String lastName;

  /** Phone number, with country code (+51987654321). Optional. */
  // TODO: validate
  String phoneNumber;

  /** Email. Optional. */
  @Email String email;

  /** Age in years, when the birthdate is not known. Optional. */
  @PositiveOrZero Integer age;

  /** Date of birth, as an ISO date (1990-05-17). Optional. */
  @PastOrPresent LocalDate birthdate;

  /** Whether the person has been baptized. Optional. */
  Boolean hasBeenBaptized;

  /**
   * Accepts WhatsApp notices. Optional: absent keeps the current choice (true for a new person).
   */
  Boolean whatsappConsent;

  /**
   * How involved the person is: visitor, membership in progress or member. Optional (a new person
   * is a visitor); whoever holds a position is a member whatever it says.
   */
  MemberState memberState;

  /** Ids of positions (cargos) of the church; others are ignored. */
  Set<UUID> positionIds = new HashSet<>();

  /** Id of the node (church, location, group or ministry) the person is registered in. */
  @NotNull UUID organizationId;
}
