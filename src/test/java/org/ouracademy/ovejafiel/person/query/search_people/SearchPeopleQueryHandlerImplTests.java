package org.ouracademy.ovejafiel.person.query.search_people;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;
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
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleResult.Standing;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SearchPeopleQueryHandlerImplTests {

  static final PageRequest PAGE = PageRequest.of(0, 20);
  static final List<AccountabilityTypeId> STATES = List.of(MEMBER_PROGRESS_STATES);

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
    when(personRepository.findByOrgIdsInState(
            ids, isPartOf, MEMBER, MEMBERSHIP_IN_PROGRESS, STATES, 2, PAGE))
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
    when(personRepository.findByNameAndOrgIdsInState(
            "an", ids, isPartOf, MEMBER, MEMBERSHIP_IN_PROGRESS, STATES, 3, PAGE))
        .thenReturn(new PageImpl<>(List.of(person(ana, "Ana", "Diaz"))));

    var result =
        handler.handle(new SearchPeopleQuery(organizationId, "an", MemberState.MEMBER, PAGE));

    assertThat(result.people().getContent()).hasSize(1);
  }

  @Test
  void eachPersonComesWithTheHighestStateInTheNodeAndBelowAndWhetherTheyLeadIt() {
    var organizationId = PartyId.generate();
    var below = PartyId.generate();
    var ana = PartyId.generate();
    var luis = PartyId.generate();
    var eva = PartyId.generate();
    var ids = subtreeOf(organizationId);
    when(personRepository.findByOrgIds(ids, isPartOf, PAGE))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    person(ana, "Ana", "Diaz"),
                    person(luis, "Luis", "Paz"),
                    person(eva, "Eva", "Rios"))));
    when(personRepository.findStandings(any(), any(), any()))
        .thenReturn(
            List.of(
                // Ana: attendee here and leads the node, but a member of a group below.
                standing(ana, organizationId, MEMBERSHIP_IN_PROGRESS),
                standing(ana, organizationId, LEADER),
                standing(ana, below, MEMBER),
                standing(luis, organizationId, MEMBER),
                // Eva leads a group below, not this node.
                standing(eva, below, MEMBERSHIP_IN_PROGRESS),
                standing(eva, below, LEADER)));

    var result = handler.handle(new SearchPeopleQuery(organizationId, null, PAGE));

    assertThat(result.standings().get(ana)).isEqualTo(new Standing(MemberState.MEMBER, true));
    assertThat(result.standings().get(luis)).isEqualTo(new Standing(MemberState.MEMBER, false));
    assertThat(result.standings().get(eva))
        .isEqualTo(new Standing(MemberState.MEMBERSHIP_IN_PROGRESS, false));
  }

  private static PersonRepository.Standing standing(
      PartyId person, PartyId node, AccountabilityTypeId type) {
    return new PersonRepository.Standing() {
      @Override
      public PartyId getPersonId() {
        return person;
      }

      @Override
      public PartyId getNodeId() {
        return node;
      }

      @Override
      public AccountabilityTypeId getTypeId() {
        return type;
      }
    };
  }
}
