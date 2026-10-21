package org.ouracademy.srtomy.accountability.command.sync_person_roles;

import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Makes the person's ROLE accountabilities in the location match exactly {@code newRoleTypeIds}.
 */
public record SyncPersonRolesCommand(
    PartyId personId, PartyId locationId, Set<AccountabilityTypeId> newRoleTypeIds) {}
