package org.ouracademy.ovejafiel.person.query.find_people_by_email;

import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record FindPeopleByEmailResult(List<PartyId> personIds) {}
