package org.ouracademy.srtomy.accountability.query.get_predefined_roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.withId;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.PASTOR;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.USHER;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.WORSHIP_LEADER;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;

@ExtendWith(MockitoExtension.class)
class GetPredefinedRolesQueryHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;

  private static AccountabilityType predefined(String name, AccountabilityTypeId id) {
    return withId(new AccountabilityType(name, null, Category.ROLE), id);
  }

  @Test
  void getPredefinedRolesReturnsTheSeededRolesAsPredefinedResults() {
    when(accountabilityTypeRepository.findAllById(List.of(PASTOR, WORSHIP_LEADER, USHER)))
        .thenReturn(
            List.of(
                predefined("PASTOR", PASTOR),
                predefined("WORSHIP_LEADER", WORSHIP_LEADER),
                predefined("USHER", USHER)));
    var handler = new GetPredefinedRolesQueryHandlerImpl(accountabilityTypeRepository);

    var result = handler.handle(new GetPredefinedRolesQuery());

    assertThat(result)
        .containsExactly(
            new AccountabilityTypeResult(PASTOR, "PASTOR", true),
            new AccountabilityTypeResult(WORSHIP_LEADER, "WORSHIP_LEADER", true),
            new AccountabilityTypeResult(USHER, "USHER", true));
  }
}
