package org.ouracademy.ovejafiel.accountability.command.set_node_membership;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Puts {@code personId} in {@code organizationId} with {@code state} (one of the member progress
 * states) and, when {@code leader}, as one of its leaders; or changes that if they are already
 * there. Nothing happens in the nodes above it: being part of a group does not make anyone part of
 * its church (that would also give them the access of a member there).
 */
public record SetNodeMembershipCommand(
    PartyId personId, PartyId organizationId, AccountabilityTypeId state, boolean leader) {}
