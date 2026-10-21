package org.ouracademy.srtomy.accountability.command;

import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Makes the person's ROLE accountabilities in the location match exactly {@code newRoleTypeIds}.
 */
public record SyncPersonRolesCommand(
    PartyId personId, PartyId locationId, Set<AccountabilityTypeId> newRoleTypeIds) {}
