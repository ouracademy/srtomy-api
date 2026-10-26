package org.ouracademy.ovejafiel.organization.query;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.springframework.stereotype.Component;

/** Resolves many party ids to {@link PersonDetails} with a single person-module call. */
@Component
@RequiredArgsConstructor
public class PersonDetailsResolver {

  private final FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;

  /** Ids that are not people (e.g. organizations) are absent from the returned map. */
  public Map<PartyId, PersonDetails> resolve(Collection<PartyId> partyIds) {
    var ids = partyIds.stream().distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(ids)).stream()
        .collect(toMap(FindPersonResult::id, PersonDetails::from, (first, second) -> first));
  }
}
