package org.ouracademy.srtomy.accountability.query.get_person_accountabilities;

import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.party.domain.PartyId;

/** {@code position} is set when the accountability is a position (cargo), and null otherwise. */
public record PersonAccountabilityResult(
    PartyId organizationId, AccountabilityTypeId typeId, PositionResult position) {}
