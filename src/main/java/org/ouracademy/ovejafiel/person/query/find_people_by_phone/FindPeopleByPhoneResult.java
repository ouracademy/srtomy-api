package org.ouracademy.ovejafiel.person.query.find_people_by_phone;

import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record FindPeopleByPhoneResult(List<Match> people) {

  public record Match(PartyId id, String name) {}
}
