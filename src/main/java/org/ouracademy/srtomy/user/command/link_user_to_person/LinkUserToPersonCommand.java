package org.ouracademy.srtomy.user.command.link_user_to_person;

import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;

/**
 * Makes {@code personId} the person that represents {@code userId} in the church {@code churchId}.
 */
public record LinkUserToPersonCommand(UserId userId, PartyId personId, PartyId churchId) {}
