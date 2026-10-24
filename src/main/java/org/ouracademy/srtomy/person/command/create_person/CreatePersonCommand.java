package org.ouracademy.srtomy.person.command.create_person;

import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.command.PersonInfo;

/**
 * Creates a person as a VISITOR that IS_PART_OF {@code organizationId}, holding exactly {@code
 * positionIds} there (ids that are not positions of the node's church are ignored).
 */
public record CreatePersonCommand(
    PersonInfo info, PartyId organizationId, Set<AccountabilityTypeId> positionIds) {}
