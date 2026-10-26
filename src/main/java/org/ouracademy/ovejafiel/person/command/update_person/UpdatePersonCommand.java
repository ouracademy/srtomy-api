package org.ouracademy.ovejafiel.person.command.update_person;

import java.util.Set;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;

/**
 * Replaces the person's data and makes their positions in {@code organizationId} match {@code
 * positionIds}. {@code subject} is the user who asks; adding or removing a position that carries
 * access is checked against them.
 */
public record UpdatePersonCommand(
    String subject,
    PartyId personId,
    PersonInfo info,
    PartyId organizationId,
    Set<AccountabilityTypeId> positionIds) {}
