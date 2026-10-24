package org.ouracademy.srtomy.accountability.command.sync_person_positions;

import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Makes {@code personId} hold exactly {@code positionIds} in {@code organizationId}. Ids that are
 * not positions of {@code churchId} are ignored, so a position of another church is never attached.
 *
 * <p>A position can give access, so adding or removing one that does is checked against {@code
 * subject}, the user who asks: it needs {@code iam.manage} on the node and every permission of the
 * position's level.
 */
public record SyncPersonPositionsCommand(
    String subject,
    PartyId personId,
    PartyId organizationId,
    PartyId churchId,
    Set<AccountabilityTypeId> positionIds) {}
