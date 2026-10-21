package org.ouracademy.srtomy.accountability.query.get_roles_of_person;

import org.ouracademy.srtomy.party.domain.PartyId;

public record GetRolesOfPersonQuery(PartyId locationId, PartyId personId) {}
