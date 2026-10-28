package org.ouracademy.ovejafiel.person.command.update_person;

import java.util.Set;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.domain.MemberState;

/**
 * Replaces the person's data and makes their positions in {@code organizationId} match {@code
 * positionIds}. {@code subject} is the user who asks; adding or removing a position that carries
 * access is checked against them. {@code memberState}, when given, is the new progress state;
 * holding a position makes them a member whatever it says.
 */
public record UpdatePersonCommand(
    String subject,
    PartyId personId,
    PersonInfo info,
    PartyId organizationId,
    Set<AccountabilityTypeId> positionIds,
    MemberState memberState) {

  /** An update that does not touch the progress state. */
  public UpdatePersonCommand(
      String subject,
      PartyId personId,
      PersonInfo info,
      PartyId organizationId,
      Set<AccountabilityTypeId> positionIds) {
    this(subject, personId, info, organizationId, positionIds, null);
  }
}
