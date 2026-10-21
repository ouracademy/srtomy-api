package org.ouracademy.srtomy.accountability.query.get_roles_of_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.withId;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.PASTOR;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class GetRolesOfPersonQueryHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;

  @Test
  void getRolesOfPersonReturnsOnlyTheRoleAccountabilityTypes() {
    var locationId = PartyId.generate();
    var personId = PartyId.generate();
    var location = party(locationId, "Location");
    var person = party(personId, "Person");
    var pastor = withId(new AccountabilityType("PASTOR", null, Category.ROLE), PASTOR);
    var memberState =
        withId(
            new AccountabilityType("VISITOR", null, Category.MEMBER_PROGRESS_STATE),
            AccountabilityTypeId.generate());
    var uncategorized =
        withId(new AccountabilityType("IS_PART_OF", null, null), AccountabilityTypeId.generate());
    when(accountabilityRepository.findByParentIdAndChildId(locationId, personId))
        .thenReturn(
            List.of(
                withId(new Accountability(person, location, pastor), AccountabilityId.generate()),
                withId(
                    new Accountability(person, location, memberState), AccountabilityId.generate()),
                withId(
                    new Accountability(person, location, uncategorized),
                    AccountabilityId.generate())));
    var handler = new GetRolesOfPersonQueryHandlerImpl(accountabilityRepository);

    var result = handler.handle(new GetRolesOfPersonQuery(locationId, personId));

    assertThat(result).containsExactly(new AccountabilityTypeResult(PASTOR, "PASTOR", true));
  }
}
