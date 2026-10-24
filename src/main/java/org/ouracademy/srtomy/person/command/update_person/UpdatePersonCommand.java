package org.ouracademy.srtomy.person.command.update_person;

import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.command.PersonInfo;

/**
 * Replaces the person's data and makes their positions in {@code organizationId} match {@code
 * positionIds}.
 */
public record UpdatePersonCommand(
    PartyId personId,
    PersonInfo info,
    PartyId organizationId,
    Set<AccountabilityTypeId> positionIds) {}
