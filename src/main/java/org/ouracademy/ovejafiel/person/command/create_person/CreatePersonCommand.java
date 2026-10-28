package org.ouracademy.ovejafiel.person.command.create_person;

import java.util.Set;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.domain.MemberState;

/**
 * Creates a person that IS_PART_OF {@code organizationId} in the progress state {@code memberState}
 * (a visitor when null), holding exactly {@code positionIds} there. Holding a position makes them a
 * member whatever the state asked (ids that are not positions of the node's church are ignored).
 * {@code subject} is the user who asks; giving a position that carries access is checked against
 * them.
 */
public record CreatePersonCommand(
    String subject,
    PersonInfo info,
    PartyId organizationId,
    Set<AccountabilityTypeId> positionIds,
    MemberState memberState) {

  /** A new person with no stated progress is a visitor. */
  public CreatePersonCommand(
      String subject,
      PersonInfo info,
      PartyId organizationId,
      Set<AccountabilityTypeId> positionIds) {
    this(subject, info, organizationId, positionIds, null);
  }
}
