package org.ouracademy.ovejafiel.organization.query.find_organization_subtree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.organization.repository.OrganizationRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindOrganizationSubtreeQueryHandlerImplTests {

  @Mock OrganizationRepository organizationRepository;
  @InjectMocks FindOrganizationSubtreeQueryHandlerImpl handler;

  @Test
  void handleReturnsTheNodeAndItsDescendants() {
    var root = PartyId.generate();
    var child = UUID.randomUUID();
    when(organizationRepository.findSubtreeIds(root.value()))
        .thenReturn(List.of(root.value(), child));

    var result = handler.handle(new FindOrganizationSubtreeQuery(root));

    assertThat(result.ids()).containsExactly(root, new PartyId(child));
  }
}
