package org.ouracademy.ovejafiel.accountability.query;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * A person who holds a position, in the node named {@code organizationName}. {@code email} is the
 * email of the user linked to the person, null when nobody is.
 */
public record PositionHolderResult(
    PartyId personId, String name, String organizationName, String email) {}
