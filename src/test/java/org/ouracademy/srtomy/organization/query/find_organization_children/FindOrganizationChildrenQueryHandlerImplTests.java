package org.ouracademy.srtomy.organization.query.find_organization_children;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindOrganizationChildrenQueryHandlerImplTests {

  @Mock OrganizationRepository organizationRepository;
  @InjectMocks FindOrganizationChildrenQueryHandlerImpl handler;

  @Test
  void handleReturnsTheDirectChildrenWithTheirParent() {
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    var a = organization(PartyId.generate(), "A", Organization.Type.LOCATION, church);
    var b = organization(PartyId.generate(), "B", Organization.Type.LOCATION, church);
    when(organizationRepository.findByParentIdOrderByName(church.getId()))
        .thenReturn(List.of(a, b));

    var result = handler.handle(new FindOrganizationChildrenQuery(church.getId()));

    assertThat(result).extracting(FindOrganizationResult::name).containsExactly("A", "B");
    assertThat(result).extracting(FindOrganizationResult::parentId).containsOnly(church.getId());
  }
}
