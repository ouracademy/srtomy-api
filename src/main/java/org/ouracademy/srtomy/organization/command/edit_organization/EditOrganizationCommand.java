package org.ouracademy.srtomy.organization.command.edit_organization;

import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.party.domain.PartyId;

public record EditOrganizationCommand(PartyId id, OrganizationInfo info) {}
