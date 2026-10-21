package org.ouracademy.srtomy.organization.command;

import org.ouracademy.srtomy.party.domain.PartyId;

public record EditOrganizationCommand(PartyId id, OrganizationInfo info) {}
