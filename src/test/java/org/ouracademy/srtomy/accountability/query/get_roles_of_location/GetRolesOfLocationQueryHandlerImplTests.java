package org.ouracademy.srtomy.accountability.query.get_roles_of_location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.withId;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.PASTOR;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.USHER;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.WORSHIP_LEADER;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;
import org.ouracademy.srtomy.accountability.query.get_predefined_roles.GetPredefinedRolesQuery;
import org.ouracademy.srtomy.accountability.query.get_predefined_roles.GetPredefinedRolesQueryHandler;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class GetRolesOfLocationQueryHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock GetPredefinedRolesQueryHandler getPredefinedRolesQueryHandler;
  @Mock EntityManager entityManager;

  @Test
  void getRolesOfLocationReturnsTheLocationRolesFollowedByThePredefinedOnes() {
    var predefinedRoles =
        List.of(
            new AccountabilityTypeResult(PASTOR, "PASTOR", true),
            new AccountabilityTypeResult(WORSHIP_LEADER, "WORSHIP_LEADER", true),
            new AccountabilityTypeResult(USHER, "USHER", true));
    when(getPredefinedRolesQueryHandler.handle(new GetPredefinedRolesQuery()))
        .thenReturn(predefinedRoles);
    var locationId = PartyId.generate();
    var location = party(locationId, "Location");
    var ownRoleId = AccountabilityTypeId.generate();
    var ownRole = withId(new AccountabilityType("CHOIR", location, Category.ROLE), ownRoleId);
    when(entityManager.getReference(Party.class, locationId)).thenReturn(location);
    when(accountabilityTypeRepository.findByParty(location)).thenReturn(List.of(ownRole));
    var handler =
        new GetRolesOfLocationQueryHandlerImpl(
            accountabilityTypeRepository, getPredefinedRolesQueryHandler, entityManager);

    var result = handler.handle(new GetRolesOfLocationQuery(locationId));

    assertThat(result)
        .containsExactly(
            new AccountabilityTypeResult(ownRoleId, "CHOIR", false),
            new AccountabilityTypeResult(PASTOR, "PASTOR", true),
            new AccountabilityTypeResult(WORSHIP_LEADER, "WORSHIP_LEADER", true),
            new AccountabilityTypeResult(USHER, "USHER", true));
  }
}
