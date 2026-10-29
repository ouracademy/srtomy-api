package org.ouracademy.ovejafiel.person.command.delete_person;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Removes the person from {@code organizationId} and everything under it. When that is the church
 * itself the person is removed from the church altogether, and if a user represents them the
 * removal only happens with {@code confirmUnlinkUser}, which also unlinks that user. Nothing is
 * deleted: memberships and positions are closed and stay as history.
 */
public record DeletePersonCommand(
    PartyId personId, PartyId organizationId, boolean confirmUnlinkUser) {}
