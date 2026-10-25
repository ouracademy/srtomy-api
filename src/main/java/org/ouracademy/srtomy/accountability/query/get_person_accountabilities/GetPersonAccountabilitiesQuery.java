package org.ouracademy.srtomy.accountability.query.get_person_accountabilities;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Every accountability {@code personId} has: membership, leadership and positions. */
public record GetPersonAccountabilitiesQuery(PartyId personId) {}
