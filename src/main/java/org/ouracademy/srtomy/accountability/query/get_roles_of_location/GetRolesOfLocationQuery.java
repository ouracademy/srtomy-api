package org.ouracademy.srtomy.accountability.query.get_roles_of_location;

import org.ouracademy.srtomy.party.domain.PartyId;

/** The location's own roles followed by the predefined ones. */
public record GetRolesOfLocationQuery(PartyId locationId) {}
