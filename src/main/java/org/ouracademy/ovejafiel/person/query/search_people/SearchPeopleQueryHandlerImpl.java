package org.ouracademy.ovejafiel.person.query.search_people;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PersonRepository's JPQL joins accountability's tables (the documented read-only exception); the
 * AccountabilityType bind value is a plain {@link EntityManager#getReference} so this module never
 * touches accountability's repositories.
 */
@Service
@RequiredArgsConstructor
class SearchPeopleQueryHandlerImpl implements SearchPeopleQueryHandler {

  private final PersonRepository personRepository;
  private final EntityManager entityManager;
  private final FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public SearchPeopleResult handle(SearchPeopleQuery query) {
    var isPartOf = entityManager.getReference(AccountabilityType.class, IS_PART_OF);
    var organizationIds =
        findOrganizationSubtreeQueryHandler
            .handle(new FindOrganizationSubtreeQuery(query.organizationId()))
            .ids();
    // The order is fixed by the query, so a sort sent by the client is ignored.
    var pageable = PageRequest.of(query.pageable().getPageNumber(), query.pageable().getPageSize());
    var people = find(query, isPartOf, organizationIds, pageable);
    var results = people.map(FindPersonResult::from);
    return new SearchPeopleResult(results, states(query.organizationId(), results));
  }

  private Page<Person> find(
      SearchPeopleQuery query,
      AccountabilityType isPartOf,
      List<PartyId> organizationIds,
      PageRequest pageable) {
    if (query.state() != null) {
      var state = entityManager.getReference(AccountabilityType.class, query.state().typeId());
      return query.searchText() != null
          ? personRepository.findByNameAndOrgIdsInState(
              query.searchText(),
              organizationIds,
              isPartOf,
              query.organizationId(),
              state,
              pageable)
          : personRepository.findByOrgIdsInState(
              organizationIds, isPartOf, query.organizationId(), state, pageable);
    }
    return query.searchText() != null
        ? personRepository.findByNameAndOrgIds(
            query.searchText(), organizationIds, isPartOf, pageable)
        : personRepository.findByOrgIds(organizationIds, isPartOf, pageable);
  }

  /** The state each person has in the node itself; none for who is only in a node below it. */
  private Map<PartyId, MemberState> states(PartyId nodeId, Page<FindPersonResult> people) {
    var ids = people.getContent().stream().map(FindPersonResult::id).toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    var states = new HashMap<PartyId, MemberState>();
    for (var row : personRepository.findStates(ids, nodeId, List.of(MEMBER_PROGRESS_STATES))) {
      Arrays.stream(MemberState.values())
          .filter(state -> state.typeId().equals(row.getTypeId()))
          .findFirst()
          .ifPresent(state -> states.put(row.getPersonId(), state));
    }
    return states;
  }
}
