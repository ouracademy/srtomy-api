package org.ouracademy.ovejafiel.organization.query.get_member_state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.VISITOR;
import static org.ouracademy.ovejafiel.person.PersonFixtures.personResult;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityId;
import org.ouracademy.ovejafiel.accountability.query.get_accountabilities_by_parent_and_types.AccountabilityResult;
import org.ouracademy.ovejafiel.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQuery;
import org.ouracademy.ovejafiel.accountability.query.get_accountabilities_by_parent_and_types.GetAccountabilitiesByParentAndTypesQueryHandler;
import org.ouracademy.ovejafiel.organization.query.PersonDetails;
import org.ouracademy.ovejafiel.organization.query.PersonDetailsResolver;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;

@ExtendWith(MockitoExtension.class)
class GetMemberStateQueryHandlerImplTests {

  @Mock GetAccountabilitiesByParentAndTypesQueryHandler accountabilitiesQueryHandler;
  @Mock FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;

  GetMemberStateQueryHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new GetMemberStateQueryHandlerImpl(
            accountabilitiesQueryHandler, new PersonDetailsResolver(findPeopleByIdsQueryHandler));
  }

  @Test
  void handleEnrichesPeopleWithTheirDetailsAndLeavesOrganizationsBare() {
    var orgId = PartyId.generate();
    var ana = PartyId.generate();
    var group = PartyId.generate();
    var visitor =
        new AccountabilityResult(AccountabilityId.generate(), ana, "Ana Diaz", orgId, VISITOR, "V");
    var member =
        new AccountabilityResult(AccountabilityId.generate(), group, "Kids", orgId, MEMBER, "M");
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(orgId, List.of(MEMBER_PROGRESS_STATES))))
        .thenReturn(List.of(visitor, member));
    var anaResult = personResult(ana, "Ana", "Diaz");
    when(findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(List.of(ana, group))))
        .thenReturn(List.of(anaResult));

    var result = handler.handle(new GetMemberStateQuery(orgId));

    assertThat(result)
        .containsExactly(
            new MemberStateResult(
                visitor.id(), ana, "Ana Diaz", orgId, VISITOR, "V", PersonDetails.from(anaResult)),
            new MemberStateResult(member.id(), group, "Kids", orgId, MEMBER, "M", null));
  }

  @Test
  void handleDoesNotCallThePersonModuleWhenThereAreNoStates() {
    var orgId = PartyId.generate();
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(orgId, List.of(MEMBER_PROGRESS_STATES))))
        .thenReturn(List.of());

    assertThat(handler.handle(new GetMemberStateQuery(orgId))).isEmpty();
    verifyNoInteractions(findPeopleByIdsQueryHandler);
  }
}
