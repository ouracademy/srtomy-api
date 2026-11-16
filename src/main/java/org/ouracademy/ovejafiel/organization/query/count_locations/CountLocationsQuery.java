package org.ouracademy.ovejafiel.organization.query.count_locations;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** How many locations hang from the church {@code churchId}. */
public record CountLocationsQuery(PartyId churchId) {}
