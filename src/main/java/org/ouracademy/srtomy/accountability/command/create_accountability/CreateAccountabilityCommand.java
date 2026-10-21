package org.ouracademy.srtomy.accountability.command.create_accountability;

import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

public record CreateAccountabilityCommand(
    PartyId childId, PartyId parentId, AccountabilityTypeId typeId) {}
