package org.ouracademy.ovejafiel.person.query.search_people;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
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
    var people =
        query.searchText() != null
            ? personRepository.findByNameAndOrgIds(
                query.searchText(), organizationIds, isPartOf, pageable)
            : personRepository.findByOrgIds(organizationIds, isPartOf, pageable);
    return new SearchPeopleResult(people.map(FindPersonResult::from));
  }
}
