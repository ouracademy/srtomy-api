package org.ouracademy.srtomy.organization.command.delete_organization;

import org.ouracademy.srtomy.party.domain.PartyId;

public record DeleteOrganizationCommand(PartyId id) {}
