package org.ouracademy.ovejafiel.accountability.command.set_member_state;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Makes {@code state} (one of the member progress states) the only state {@code personId} has in
 * {@code organizationId}: visitor, membership in progress or member.
 */
public record SetMemberStateCommand(
    PartyId personId, PartyId organizationId, AccountabilityTypeId state) {}
