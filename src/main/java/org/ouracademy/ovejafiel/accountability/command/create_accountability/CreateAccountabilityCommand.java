package org.ouracademy.ovejafiel.accountability.command.create_accountability;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record CreateAccountabilityCommand(
    PartyId childId, PartyId parentId, AccountabilityTypeId typeId) {}
