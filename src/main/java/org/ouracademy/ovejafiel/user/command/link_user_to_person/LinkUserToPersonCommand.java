package org.ouracademy.ovejafiel.user.command.link_user_to_person;

import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * Makes {@code personId} the person that represents {@code userId} in the church {@code churchId}.
 */
public record LinkUserToPersonCommand(UserId userId, PartyId personId, PartyId churchId) {}
