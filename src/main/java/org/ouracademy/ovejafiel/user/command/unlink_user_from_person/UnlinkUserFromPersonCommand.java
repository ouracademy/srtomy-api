package org.ouracademy.ovejafiel.user.command.unlink_user_from_person;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Stops the user that represents {@code personId} from doing so. The user (their login) is kept: it
 * only loses the access that came from the positions of that person.
 */
public record UnlinkUserFromPersonCommand(PartyId personId) {}
