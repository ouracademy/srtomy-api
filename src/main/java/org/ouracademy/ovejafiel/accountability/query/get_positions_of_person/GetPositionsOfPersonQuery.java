package org.ouracademy.ovejafiel.accountability.query.get_positions_of_person;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The positions {@code personId} holds in {@code organizationId}. */
public record GetPositionsOfPersonQuery(PartyId organizationId, PartyId personId) {}
