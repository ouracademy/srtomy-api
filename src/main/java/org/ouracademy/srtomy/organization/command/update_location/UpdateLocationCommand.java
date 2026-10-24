package org.ouracademy.srtomy.organization.command.update_location;

import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Overwrites a location's info. */
public record UpdateLocationCommand(PartyId id, OrganizationInfo info) {}
