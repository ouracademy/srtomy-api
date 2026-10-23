package org.ouracademy.srtomy.accountability.command.delete_accountabilities_by_parent_and_types;

import java.util.List;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Deletes the accountabilities whose parent is {@code parentId} and whose type is in {@code
 * typeIds}.
 */
public record DeleteAccountabilitiesByParentAndTypesCommand(
    PartyId parentId, List<AccountabilityTypeId> typeIds) {}
