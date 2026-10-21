package org.ouracademy.srtomy.organization.query.find_organization_children;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

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
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindOrganizationChildrenQueryHandlerImplTests {

  @Mock GetAccountabilitiesByParentAndTypesQueryHandler accountabilitiesQueryHandler;
  @Mock OrganizationRepository organizationRepository;

  FindOrganizationChildrenQueryHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new FindOrganizationChildrenQueryHandlerImpl(
            accountabilitiesQueryHandler, organizationRepository);
  }

  static AccountabilityResult isPartOf(PartyId childId, String childName, PartyId parentId) {
    return new AccountabilityResult(
        AccountabilityId.generate(), childId, childName, parentId, IS_PART_OF, "IS_PART_OF");
  }

  @Test
  void handleKeepsOnlyTheChildrenThatAreOrganizationsInAccountabilityOrder() {
    var parentId = PartyId.generate();
    var locationB = PartyId.generate();
    var person = PartyId.generate();
    var locationA = PartyId.generate();
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(parentId, List.of(IS_PART_OF))))
        .thenReturn(
            List.of(
                isPartOf(locationB, "B", parentId),
                isPartOf(person, "Ana", parentId),
                isPartOf(locationA, "A", parentId)));
    when(organizationRepository.findAllById(List.of(locationB, person, locationA)))
        .thenReturn(
            List.of(
                organization(locationA, "A", Organization.Type.LOCATION),
                organization(locationB, "B", Organization.Type.LOCATION)));

    var result = handler.handle(new FindOrganizationChildrenQuery(parentId));

    assertThat(result).extracting(FindOrganizationResult::id).containsExactly(locationB, locationA);
    assertThat(result).extracting(FindOrganizationResult::name).containsExactly("B", "A");
  }

  @Test
  void handleSkipsTheOrganizationLookupWhenThereAreNoChildren() {
    var parentId = PartyId.generate();
    when(accountabilitiesQueryHandler.handle(
            new GetAccountabilitiesByParentAndTypesQuery(parentId, List.of(IS_PART_OF))))
        .thenReturn(List.of());

    assertThat(handler.handle(new FindOrganizationChildrenQuery(parentId))).isEmpty();
    verifyNoInteractions(organizationRepository);
  }
}
