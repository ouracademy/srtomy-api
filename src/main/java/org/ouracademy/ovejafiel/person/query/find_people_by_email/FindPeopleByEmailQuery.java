package org.ouracademy.ovejafiel.person.query.find_people_by_email;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The people of {@code organizationId} (members, leaders or position holders) with that email. */
public record FindPeopleByEmailQuery(String email, PartyId organizationId) {}
