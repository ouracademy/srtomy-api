package org.ouracademy.ovejafiel.person.command.create_person;

import java.util.Set;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;

/**
 * Creates a person as a VISITOR that IS_PART_OF {@code organizationId}, holding exactly {@code
 * positionIds} there (ids that are not positions of the node's church are ignored). {@code subject}
 * is the user who asks; giving a position that carries access is checked against them.
 */
public record CreatePersonCommand(
    String subject,
    PersonInfo info,
    PartyId organizationId,
    Set<AccountabilityTypeId> positionIds) {}
