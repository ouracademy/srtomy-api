package org.ouracademy.srtomy.accountability.query;

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
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class RoleQueryHandlersTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AccountabilityRepository accountabilityRepository;
  @Mock EntityManager entityManager;

  private static AccountabilityType predefined(String name, AccountabilityTypeId id) {
    return withId(new AccountabilityType(name, null, Category.ROLE), id);
  }

  private List<AccountabilityType> stubPredefinedRoles() {
    var roles =
        List.of(
            predefined("PASTOR", PASTOR),
            predefined("WORSHIP_LEADER", WORSHIP_LEADER),
            predefined("USHER", USHER));
    when(accountabilityTypeRepository.findAllById(List.of(PASTOR, WORSHIP_LEADER, USHER)))
        .thenReturn(roles);
    return roles;
  }

  @Test
  void getPredefinedRolesReturnsTheSeededRolesAsPredefinedResults() {
    stubPredefinedRoles();
    var handler = new GetPredefinedRolesQueryHandlerImpl(accountabilityTypeRepository);

    var result = handler.handle(new GetPredefinedRolesQuery());

    assertThat(result)
        .containsExactly(
            new AccountabilityTypeResult(PASTOR, "PASTOR", true),
            new AccountabilityTypeResult(WORSHIP_LEADER, "WORSHIP_LEADER", true),
            new AccountabilityTypeResult(USHER, "USHER", true));
  }

  @Test
  void getRolesOfLocationReturnsTheLocationRolesFollowedByThePredefinedOnes() {
    stubPredefinedRoles();
    var locationId = PartyId.generate();
    var location = party(locationId, "Location");
    var ownRoleId = AccountabilityTypeId.generate();
    var ownRole = withId(new AccountabilityType("CHOIR", location, Category.ROLE), ownRoleId);
    when(entityManager.getReference(Party.class, locationId)).thenReturn(location);
    when(accountabilityTypeRepository.findByParty(location)).thenReturn(List.of(ownRole));
    var handler =
        new GetRolesOfLocationQueryHandlerImpl(
            accountabilityTypeRepository,
            new GetPredefinedRolesQueryHandlerImpl(accountabilityTypeRepository),
            entityManager);

    var result = handler.handle(new GetRolesOfLocationQuery(locationId));

    assertThat(result)
        .containsExactly(
            new AccountabilityTypeResult(ownRoleId, "CHOIR", false),
            new AccountabilityTypeResult(PASTOR, "PASTOR", true),
            new AccountabilityTypeResult(WORSHIP_LEADER, "WORSHIP_LEADER", true),
            new AccountabilityTypeResult(USHER, "USHER", true));
  }

  @Test
  void getRolesOfPersonReturnsOnlyTheRoleAccountabilityTypes() {
    var locationId = PartyId.generate();
    var personId = PartyId.generate();
    var location = party(locationId, "Location");
    var person = party(personId, "Person");
    var pastor = predefined("PASTOR", PASTOR);
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
