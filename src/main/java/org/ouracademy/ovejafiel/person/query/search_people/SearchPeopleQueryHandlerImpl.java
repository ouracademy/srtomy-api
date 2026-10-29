package org.ouracademy.ovejafiel.person.query.search_people;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;
import static org.ouracademy.ovejafiel.person.domain.MemberState.MEMBER;
import static org.ouracademy.ovejafiel.person.domain.MemberState.MEMBERSHIP_IN_PROGRESS;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleResult.Standing;
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
    return new SearchPeopleResult(
        results, standings(query.organizationId(), organizationIds, results));
  }

  private Page<Person> find(
      SearchPeopleQuery query,
      AccountabilityType isPartOf,
      List<PartyId> organizationIds,
      PageRequest pageable) {
    if (query.state() != null) {
      var states = List.of(MEMBER_PROGRESS_STATES);
      var rank = query.state().rank();
      return query.searchText() != null
          ? personRepository.findByNameAndOrgIdsInState(
              query.searchText(),
              organizationIds,
              isPartOf,
              MEMBER.typeId(),
              MEMBERSHIP_IN_PROGRESS.typeId(),
              states,
              rank,
              pageable)
          : personRepository.findByOrgIdsInState(
              organizationIds,
              isPartOf,
              MEMBER.typeId(),
              MEMBERSHIP_IN_PROGRESS.typeId(),
              states,
              rank,
              pageable);
    }
    return query.searchText() != null
        ? personRepository.findByNameAndOrgIds(
            query.searchText(), organizationIds, isPartOf, pageable)
        : personRepository.findByOrgIds(organizationIds, isPartOf, pageable);
  }

  /** The highest state of each person in the node and below it, and whether they lead the node. */
  private Map<PartyId, Standing> standings(
      PartyId nodeId, List<PartyId> organizationIds, Page<FindPersonResult> people) {
    var ids = people.getContent().stream().map(FindPersonResult::id).toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    var typeIds = new ArrayList<>(Arrays.asList(MEMBER_PROGRESS_STATES));
    typeIds.add(LEADER);
    var states = new HashMap<PartyId, MemberState>();
    var leaders = new HashSet<PartyId>();
    for (var row : personRepository.findStandings(ids, organizationIds, typeIds)) {
      if (row.getTypeId().equals(LEADER)) {
        if (row.getNodeId().equals(nodeId)) {
          leaders.add(row.getPersonId());
        }
      } else {
        stateOf(row.getTypeId())
            .ifPresent(
                state ->
                    states.merge(row.getPersonId(), state, SearchPeopleQueryHandlerImpl::higher));
      }
    }
    var result = new HashMap<PartyId, Standing>();
    states.forEach((id, state) -> result.put(id, new Standing(state, leaders.contains(id))));
    return result;
  }

  private static MemberState higher(MemberState a, MemberState b) {
    return a.rank() >= b.rank() ? a : b;
  }

  private static Optional<MemberState> stateOf(AccountabilityTypeId typeId) {
    return Arrays.stream(MemberState.values()).filter(s -> s.typeId().equals(typeId)).findFirst();
  }
}
