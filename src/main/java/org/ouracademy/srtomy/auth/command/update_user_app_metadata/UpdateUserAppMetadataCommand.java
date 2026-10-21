package org.ouracademy.srtomy.auth.command.update_user_app_metadata;

import org.ouracademy.srtomy.party.domain.PartyId;

public record UpdateUserAppMetadataCommand(String userId, PartyId organizationId) {}
