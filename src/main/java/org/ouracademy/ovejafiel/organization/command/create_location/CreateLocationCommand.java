package org.ouracademy.ovejafiel.organization.command.create_location;

import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Creates a location under the church {@code parentId}. */
public record CreateLocationCommand(OrganizationInfo info, PartyId parentId) {}
