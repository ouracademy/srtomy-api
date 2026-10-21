package org.ouracademy.srtomy.organization.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;
import static org.ouracademy.srtomy.person.PersonFixtures.personResult;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.query.AccountabilityResult;
import org.ouracademy.srtomy.accountability.query.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.srtomy.accountability.query.GetAccountabilitiesByParentAndTypesQueryHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.GetLocationChildrenResult.Child;
import org.ouracademy.srtomy.organization.query.GetLocationChildrenResult.Kind;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.query.FindPeopleByIdsQuery;
import org.ouracademy.srtomy.person.query.FindPeopleByIdsQueryHandler;

@ExtendWith(MockitoExtension.class)
class GetLocationChildrenQueryHandlerImplTests {

  @Mock GetAccountabilitiesByParentAndTypesQueryHandler accountabilitiesQueryHandler;
  @Mock OrganizationRepository organizationRepository;
  @Mock FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;

  GetLocationChildrenQueryHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new GetLocationChildrenQueryHandlerImpl(
            accountabilitiesQueryHandler,
            organizationRepository,
            new PersonDetailsResolver(findPeopleByIdsQueryHandler));
  }

  @Test
  void handleGroupsChildrenByTypeNameTypesThemAndEnrichesPeopleInOneCall() {
    var groupId = PartyId.generate();
    var member = PartyId.generate();
    var subGroup = PartyId.generate();
    var leader = PartyId.generate();
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(groupId, List.of(IS_PART_OF, LEADER))))
        .thenReturn(
            List.of(
                new AccountabilityResult(
                    AccountabilityId.generate(), member, "Ana", groupId, IS_PART_OF, "IS_PART_OF"),
                new AccountabilityResult(
                    AccountabilityId.generate(),
                    subGroup,
                    "Kids",
                    groupId,
                    IS_PART_OF,
                    "IS_PART_OF"),
                new AccountabilityResult(
                    AccountabilityId.generate(), leader, "Luis", groupId, LEADER, "LEADER")));
    when(organizationRepository.findAllById(List.of(member, subGroup, leader)))
        .thenReturn(List.of(organization(subGroup, "Kids", Organization.Type.GROUP)));
    var ana = personResult(member, "Ana", "Diaz");
    var luis = personResult(leader, "Luis", "Rojas");
    when(findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(List.of(member, leader))))
        .thenReturn(List.of(ana, luis));

    var result = handler.handle(new GetLocationChildrenQuery(groupId));

    assertThat(result.childrenByType())
        .containsOnlyKeys("IS_PART_OF", "LEADER")
        .containsEntry(
            "IS_PART_OF",
            List.of(
                new Child(member, "Ana", Kind.PERSON, PersonDetails.from(ana)),
                new Child(subGroup, "Kids", Kind.ORGANIZATION, null)))
        .containsEntry(
            "LEADER", List.of(new Child(leader, "Luis", Kind.PERSON, PersonDetails.from(luis))));
    var anaDetails = result.childrenByType().get("IS_PART_OF").getFirst().person();
    assertThat(anaDetails.firstName()).isEqualTo("Ana");
    assertThat(anaDetails.lastName()).isEqualTo("Diaz");
    assertThat(anaDetails.age()).isEqualTo(30);
    assertThat(anaDetails.birthdate()).isEqualTo(LocalDate.of(1995, 5, 17));
  }

  @Test
  void handleDoesNotCallThePersonModuleWhenEveryChildIsAnOrganization() {
    var groupId = PartyId.generate();
    var subGroup = PartyId.generate();
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(groupId, List.of(IS_PART_OF, LEADER))))
        .thenReturn(
            List.of(
                new AccountabilityResult(
                    AccountabilityId.generate(),
                    subGroup,
                    "Kids",
                    groupId,
                    IS_PART_OF,
                    "IS_PART_OF")));
    when(organizationRepository.findAllById(List.of(subGroup)))
        .thenReturn(List.of(organization(subGroup, "Kids", Organization.Type.GROUP)));

    var result = handler.handle(new GetLocationChildrenQuery(groupId));

    assertThat(result.childrenByType())
        .containsEntry("IS_PART_OF", List.of(new Child(subGroup, "Kids", Kind.ORGANIZATION, null)));
    verifyNoInteractions(findPeopleByIdsQueryHandler);
  }
}
