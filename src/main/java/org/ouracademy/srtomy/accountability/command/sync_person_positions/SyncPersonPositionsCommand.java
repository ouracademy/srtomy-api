package org.ouracademy.srtomy.accountability.command.sync_person_positions;

import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Makes {@code personId} hold exactly {@code positionIds} in {@code organizationId}. Ids that are
 * not positions of {@code churchId} are ignored, so a position of another church is never attached.
 */
public record SyncPersonPositionsCommand(
    PartyId personId,
    PartyId organizationId,
    PartyId churchId,
    Set<AccountabilityTypeId> positionIds) {}
