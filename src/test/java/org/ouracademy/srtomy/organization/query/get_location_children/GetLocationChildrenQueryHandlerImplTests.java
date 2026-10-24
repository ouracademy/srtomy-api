package org.ouracademy.srtomy.organization.query.get_location_children;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.srtomy.person.PersonFixtures.personResult;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.AccountabilityResult;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQueryHandler;
import org.ouracademy.srtomy.organization.query.PersonDetails;
import org.ouracademy.srtomy.organization.query.PersonDetailsResolver;
import org.ouracademy.srtomy.organization.query.get_location_children.GetLocationChildrenResult.Child;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.srtomy.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;

@ExtendWith(MockitoExtension.class)
class GetLocationChildrenQueryHandlerImplTests {

  @Mock GetAccountabilitiesByParentAndTypesQueryHandler accountabilitiesQueryHandler;
  @Mock FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;

  GetLocationChildrenQueryHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new GetLocationChildrenQueryHandlerImpl(
            accountabilitiesQueryHandler, new PersonDetailsResolver(findPeopleByIdsQueryHandler));
  }

  @Test
  void handleGroupsChildrenByTypeNameAndEnrichesPeopleInOneCall() {
    var groupId = PartyId.generate();
    var member = PartyId.generate();
    var leader = PartyId.generate();
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(groupId, List.of(IS_PART_OF, LEADER))))
        .thenReturn(
            List.of(
                new AccountabilityResult(
                    AccountabilityId.generate(), member, "Ana", groupId, IS_PART_OF, "IS_PART_OF"),
                new AccountabilityResult(
                    AccountabilityId.generate(), leader, "Luis", groupId, LEADER, "LEADER")));
    var ana = personResult(member, "Ana", "Diaz");
    var luis = personResult(leader, "Luis", "Rojas");
    when(findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(List.of(member, leader))))
        .thenReturn(List.of(ana, luis));

    var result = handler.handle(new GetLocationChildrenQuery(groupId));

    assertThat(result.childrenByType())
        .containsOnlyKeys("IS_PART_OF", "LEADER")
        .containsEntry("IS_PART_OF", List.of(new Child(member, "Ana", PersonDetails.from(ana))))
        .containsEntry("LEADER", List.of(new Child(leader, "Luis", PersonDetails.from(luis))));
    var anaDetails = result.childrenByType().get("IS_PART_OF").getFirst().person();
    assertThat(anaDetails.firstName()).isEqualTo("Ana");
    assertThat(anaDetails.lastName()).isEqualTo("Diaz");
    assertThat(anaDetails.age()).isEqualTo(30);
    assertThat(anaDetails.birthdate()).isEqualTo(LocalDate.of(1995, 5, 17));
  }

  @Test
  void handleKeepsAnIdThatIsNotAPersonWithoutDetails() {
    var groupId = PartyId.generate();
    var unknown = PartyId.generate();
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(groupId, List.of(IS_PART_OF, LEADER))))
        .thenReturn(
            List.of(
                new AccountabilityResult(
                    AccountabilityId.generate(),
                    unknown,
                    "Kids",
                    groupId,
                    IS_PART_OF,
                    "IS_PART_OF")));
    when(findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(List.of(unknown))))
        .thenReturn(List.of());

    var result = handler.handle(new GetLocationChildrenQuery(groupId));

    assertThat(result.childrenByType())
        .containsEntry("IS_PART_OF", List.of(new Child(unknown, "Kids", null)));
  }

  @Test
  void handleOfAGroupWithoutMembersDoesNotCallThePersonModule() {
    var groupId = PartyId.generate();
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(groupId, List.of(IS_PART_OF, LEADER))))
        .thenReturn(List.of());

    assertThat(handler.handle(new GetLocationChildrenQuery(groupId)).childrenByType()).isEmpty();
    verifyNoInteractions(findPeopleByIdsQueryHandler);
  }
}
