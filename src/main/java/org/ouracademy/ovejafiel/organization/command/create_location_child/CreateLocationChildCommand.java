package org.ouracademy.ovejafiel.organization.command.create_location_child;

import java.util.List;
import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Creates a group or ministry under a location, group or ministry, with its members (IS_PART_OF)
 * and leaders (LEADER).
 */
public record CreateLocationChildCommand(
    OrganizationInfo info,
    PartyId parentId,
    Organization.Type type,
    List<PartyId> memberIds,
    List<PartyId> leaderIds) {}
