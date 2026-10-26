package org.ouracademy.ovejafiel.organization.command.update_location;

import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Overwrites a location's info. */
public record UpdateLocationCommand(PartyId id, OrganizationInfo info) {}
