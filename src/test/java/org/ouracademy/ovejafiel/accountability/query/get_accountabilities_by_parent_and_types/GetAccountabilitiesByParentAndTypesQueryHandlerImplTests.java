package org.ouracademy.ovejafiel.accountability.query.get_accountabilities_by_parent_and_types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.AccountabilityFixtures.party;
import static org.ouracademy.ovejafiel.accountability.AccountabilityFixtures.withId;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityId;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class GetAccountabilitiesByParentAndTypesQueryHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;

  @Test
  void handleMapsTheMatchingAccountabilitiesToResults() {
    var parentId = PartyId.generate();
    var childId = PartyId.generate();
    var parent = party(parentId, "Group");
    var child = party(childId, "Ana");
    var member = withId(new AccountabilityType("MEMBER", null, null), MEMBER);
    var accountabilityId = AccountabilityId.generate();
    var accountability = withId(new Accountability(child, parent, member), accountabilityId);
    when(accountabilityRepository.findByParentIdAndTypeIdIn(parentId, List.of(IS_PART_OF, MEMBER)))
        .thenReturn(List.of(accountability));
    var handler = new GetAccountabilitiesByParentAndTypesQueryHandlerImpl(accountabilityRepository);

    var result =
        handler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(parentId, List.of(IS_PART_OF, MEMBER)));

    assertThat(result)
        .containsExactly(
            new AccountabilityResult(accountabilityId, childId, "Ana", parentId, MEMBER, "MEMBER"));
  }
}
