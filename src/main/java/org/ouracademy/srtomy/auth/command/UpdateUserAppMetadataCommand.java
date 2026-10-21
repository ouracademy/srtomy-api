package org.ouracademy.srtomy.auth.command;

import org.ouracademy.srtomy.party.domain.PartyId;

public record UpdateUserAppMetadataCommand(String userId, PartyId organizationId) {}
