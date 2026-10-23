package org.ouracademy.srtomy.organization.command.update_location_child;

import java.util.List;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Edits a group or ministry and replaces its members (IS_PART_OF) and leaders (LEADER). */
public record UpdateLocationChildCommand(
    PartyId id,
    OrganizationInfo info,
    Organization.Type type,
    List<PartyId> memberIds,
    List<PartyId> leaderIds) {}
