package org.ouracademy.ovejafiel.accountability.command.set_node_membership;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Puts {@code personId} in {@code organizationId} with {@code state} (one of the member progress
 * states), or changes it if they are already there. {@code ancestorIds} are the nodes above it (the
 * church first): whoever is in a group is also in its church, so each of them that does not know
 * the person yet gets them with the same state. That is only a state, not a membership: being part
 * of a node gives the access of a member there and below, which joining a group must not do for the
 * whole church.
 */
public record SetNodeMembershipCommand(
    PartyId personId,
    PartyId organizationId,
    List<PartyId> ancestorIds,
    AccountabilityTypeId state) {}
