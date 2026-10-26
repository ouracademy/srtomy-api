package org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/** {@code position} is set when the accountability is a position (cargo), and null otherwise. */
public record PersonAccountabilityResult(
    PartyId organizationId, AccountabilityTypeId typeId, PositionResult position) {}
