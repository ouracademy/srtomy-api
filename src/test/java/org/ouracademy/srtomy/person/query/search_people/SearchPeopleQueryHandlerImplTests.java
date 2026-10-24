package org.ouracademy.srtomy.person.query.search_people;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.person.PersonFixtures.person;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.srtomy.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.srtomy.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.repository.PersonRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SearchPeopleQueryHandlerImplTests {

  static final PageRequest PAGE = PageRequest.of(0, 20);

  @Mock PersonRepository personRepository;
  @Mock EntityManager entityManager;
  @Mock FindOrganizationSubtreeQueryHandler subtree;
  @InjectMocks SearchPeopleQueryHandlerImpl handler;

  AccountabilityType isPartOf = new AccountabilityType("IS_PART_OF", null, null);

  @BeforeEach
  void setUp() {
    when(entityManager.getReference(AccountabilityType.class, IS_PART_OF)).thenReturn(isPartOf);
  }

  private List<PartyId> subtreeOf(PartyId organizationId) {
    var ids = List.of(organizationId, PartyId.generate());
    when(subtree.handle(new FindOrganizationSubtreeQuery(organizationId)))
        .thenReturn(new FindOrganizationSubtreeResult(ids));
    return ids;
  }

  @Test
  void handleWithoutSearchTextListsEveryPersonThatIsPartOfTheOrganization() {
    var organizationId = PartyId.generate();
    var ana = PartyId.generate();
    var ids = subtreeOf(organizationId);
    when(personRepository.findByOrgIds(ids, isPartOf, PAGE))
        .thenReturn(new PageImpl<>(List.of(person(ana, "Ana", "Diaz"))));

    var result = handler.handle(new SearchPeopleQuery(organizationId, null, PAGE));

    assertThat(result.people().getContent().get(0).id()).isEqualTo(ana);
    verify(personRepository, never()).findByNameAndOrgIds(any(), any(), any(), any());
  }

  @Test
  void handleWithSearchTextFiltersByName() {
    var organizationId = PartyId.generate();
    var ana = PartyId.generate();
    var ids = subtreeOf(organizationId);
    when(personRepository.findByNameAndOrgIds("an", ids, isPartOf, PAGE))
        .thenReturn(new PageImpl<>(List.of(person(ana, "Ana", "Diaz"))));

    var result = handler.handle(new SearchPeopleQuery(organizationId, "an", PAGE));

    assertThat(result.people().getContent().get(0).firstName()).isEqualTo("Ana");
    verify(personRepository, never()).findByOrgIds(any(), any(), any());
  }
}
