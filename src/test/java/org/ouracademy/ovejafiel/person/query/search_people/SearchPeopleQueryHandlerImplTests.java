package org.ouracademy.ovejafiel.person.query.search_people;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
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

  @Test
  void handleWithAStateOnlyListsThoseWhoHaveItInTheNode() {
    var organizationId = PartyId.generate();
    var ana = PartyId.generate();
    var ids = subtreeOf(organizationId);
    var attendee = new AccountabilityType("MEMBERSHIP_IN_PROGRESS", null, null);
    when(entityManager.getReference(AccountabilityType.class, MEMBERSHIP_IN_PROGRESS))
        .thenReturn(attendee);
    when(personRepository.findByOrgIdsInState(ids, isPartOf, organizationId, attendee, PAGE))
        .thenReturn(new PageImpl<>(List.of(person(ana, "Ana", "Diaz"))));

    var result =
        handler.handle(
            new SearchPeopleQuery(organizationId, null, MemberState.MEMBERSHIP_IN_PROGRESS, PAGE));

    assertThat(result.people().getContent().get(0).id()).isEqualTo(ana);
    verify(personRepository, never()).findByOrgIds(any(), any(), any());
  }

  @Test
  void handleWithAStateAndSearchTextFiltersByBoth() {
    var organizationId = PartyId.generate();
    var ana = PartyId.generate();
    var ids = subtreeOf(organizationId);
    var member = new AccountabilityType("MEMBER", null, null);
    when(entityManager.getReference(AccountabilityType.class, MEMBER)).thenReturn(member);
    when(personRepository.findByNameAndOrgIdsInState(
            "an", ids, isPartOf, organizationId, member, PAGE))
        .thenReturn(new PageImpl<>(List.of(person(ana, "Ana", "Diaz"))));

    var result =
        handler.handle(new SearchPeopleQuery(organizationId, "an", MemberState.MEMBER, PAGE));

    assertThat(result.people().getContent()).hasSize(1);
  }

  @Test
  void eachPersonComesWithTheStateTheyHaveInTheNode() {
    var organizationId = PartyId.generate();
    var ana = PartyId.generate();
    var luis = PartyId.generate();
    var below = PartyId.generate();
    var ids = subtreeOf(organizationId);
    when(personRepository.findByOrgIds(ids, isPartOf, PAGE))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    person(ana, "Ana", "Diaz"),
                    person(luis, "Luis", "Paz"),
                    person(below, "Eva", "Rios"))));
    when(personRepository.findStates(any(), any(), any()))
        .thenReturn(List.of(state(ana, MEMBERSHIP_IN_PROGRESS), state(luis, MEMBER)));

    var result = handler.handle(new SearchPeopleQuery(organizationId, null, PAGE));

    assertThat(result.states().get(ana)).isEqualTo(MemberState.MEMBERSHIP_IN_PROGRESS);
    assertThat(result.states().get(luis)).isEqualTo(MemberState.MEMBER);
    // Eva is only in a node below, so she has no state here.
    assertThat(result.states()).doesNotContainKey(below);
  }

  private static PersonRepository.StateRow state(PartyId person, AccountabilityTypeId type) {
    return new PersonRepository.StateRow() {
      @Override
      public PartyId getPersonId() {
        return person;
      }

      @Override
      public AccountabilityTypeId getTypeId() {
        return type;
      }
    };
  }
}
