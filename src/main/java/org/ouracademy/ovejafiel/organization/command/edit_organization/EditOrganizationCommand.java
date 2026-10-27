package org.ouracademy.ovejafiel.organization.command.edit_organization;

import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record EditOrganizationCommand(PartyId id, OrganizationInfo info) {}
