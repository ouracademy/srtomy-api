package org.ouracademy.ovejafiel.person.query.count_people;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** How many people the church {@code churchId} has in its whole tree, without the removed ones. */
public record CountPeopleQuery(PartyId churchId) {}
