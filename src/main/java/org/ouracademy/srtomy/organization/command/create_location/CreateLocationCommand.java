package org.ouracademy.srtomy.organization.command.create_location;

import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Creates a location under the church {@code parentId}. */
public record CreateLocationCommand(OrganizationInfo info, PartyId parentId) {}
