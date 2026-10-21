package org.ouracademy.srtomy.party.query;

import org.ouracademy.srtomy.party.domain.PartyId;

public record GetPartyByIdResult(PartyId id, String name) {}
