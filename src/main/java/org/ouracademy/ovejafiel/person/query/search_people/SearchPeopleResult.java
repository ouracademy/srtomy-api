package org.ouracademy.ovejafiel.person.query.search_people;

import java.util.Map;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.springframework.data.domain.Page;

/** A page of people and the state each has in the node that was searched. */
public record SearchPeopleResult(Page<FindPersonResult> people, Map<PartyId, MemberState> states) {

  public SearchPeopleResult(Page<FindPersonResult> people) {
    this(people, Map.of());
  }
}
