package org.ouracademy.ovejafiel.person.query.find_person_matches;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * The people of the church of {@code organizationId}, removed ones included, whose name contains
 * {@code name} or whose email is {@code email} (ignoring case). Either may be null or blank.
 */
public record FindPersonMatchesQuery(PartyId organizationId, String name, String email) {}
