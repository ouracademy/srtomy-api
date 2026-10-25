package org.ouracademy.srtomy.user.query.is_person_linked;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Asks whether some user already represents {@code personId}. */
public record IsPersonLinkedQuery(PartyId personId) {}
