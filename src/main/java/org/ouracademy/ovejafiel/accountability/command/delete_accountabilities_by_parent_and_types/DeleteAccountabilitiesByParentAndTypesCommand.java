package org.ouracademy.ovejafiel.accountability.command.delete_accountabilities_by_parent_and_types;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Deletes the accountabilities whose parent is {@code parentId} and whose type is in {@code
 * typeIds}.
 */
public record DeleteAccountabilitiesByParentAndTypesCommand(
    PartyId parentId, List<AccountabilityTypeId> typeIds) {}
