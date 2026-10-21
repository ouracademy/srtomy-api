package org.ouracademy.srtomy.person.query.search_people;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.person.query.FindPersonResult;
import org.ouracademy.srtomy.person.repository.PersonRepository;
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

  @Override
  @Transactional(readOnly = true)
  public SearchPeopleResult handle(SearchPeopleQuery query) {
    var isPartOf = entityManager.getReference(AccountabilityType.class, IS_PART_OF);
    var people =
        query.searchText() != null
            ? personRepository.findByNameAndOrgId(
                query.searchText(), query.organizationId(), isPartOf, query.pageable())
            : personRepository.findByOrgId(query.organizationId(), isPartOf, query.pageable());
    return new SearchPeopleResult(people.map(FindPersonResult::from));
  }
}
