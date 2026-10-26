package org.ouracademy.ovejafiel.user.query.is_person_linked;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Asks whether some user already represents {@code personId}. */
public record IsPersonLinkedQuery(PartyId personId) {}
