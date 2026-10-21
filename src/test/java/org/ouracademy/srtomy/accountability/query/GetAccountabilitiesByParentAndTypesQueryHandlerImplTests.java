package org.ouracademy.srtomy.accountability.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.withId;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class GetAccountabilitiesByParentAndTypesQueryHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;

  @Test
  void handleMapsTheMatchingAccountabilitiesToResults() {
    var parentId = PartyId.generate();
    var childId = PartyId.generate();
    var parent = party(parentId, "Group");
    var child = party(childId, "Ana");
    var leader = withId(new AccountabilityType("LEADER", null, null), LEADER);
    var accountabilityId = AccountabilityId.generate();
    var accountability = withId(new Accountability(child, parent, leader), accountabilityId);
    when(accountabilityRepository.findByParentIdAndTypeIdIn(parentId, List.of(IS_PART_OF, LEADER)))
        .thenReturn(List.of(accountability));
    var handler = new GetAccountabilitiesByParentAndTypesQueryHandlerImpl(accountabilityRepository);

    var result =
        handler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(parentId, List.of(IS_PART_OF, LEADER)));

    assertThat(result)
        .containsExactly(
            new AccountabilityResult(accountabilityId, childId, "Ana", parentId, LEADER, "LEADER"));
  }
}
