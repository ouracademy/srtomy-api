package org.ouracademy.ovejafiel.person.query.search_people;

import java.util.Map;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.springframework.data.domain.Page;

/**
 * A page of people and, for each, how they stand in the node that was searched: {@code state} is
 * the highest they have in that node or any node below it, and {@code leader} whether they lead the
 * node itself.
 */
public record SearchPeopleResult(Page<FindPersonResult> people, Map<PartyId, Standing> standings) {

  public record Standing(MemberState state, boolean leader) {}

  public SearchPeopleResult(Page<FindPersonResult> people) {
    this(people, Map.of());
  }
}
