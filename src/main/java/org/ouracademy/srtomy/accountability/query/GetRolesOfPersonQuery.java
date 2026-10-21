package org.ouracademy.srtomy.accountability.query;

import org.ouracademy.srtomy.party.domain.PartyId;

public record GetRolesOfPersonQuery(PartyId locationId, PartyId personId) {}
