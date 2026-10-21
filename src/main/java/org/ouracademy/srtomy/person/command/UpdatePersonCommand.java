package org.ouracademy.srtomy.person.command;

import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Replaces the person's data and makes their roles in {@code organizationId} match {@code roles}.
 */
public record UpdatePersonCommand(
    PartyId personId, PersonInfo info, PartyId organizationId, Set<AccountabilityTypeId> roles) {}
