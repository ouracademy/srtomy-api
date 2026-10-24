package org.ouracademy.srtomy.organization.query.find_church_of_organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindChurchOfOrganizationQueryHandlerImplTests {

  @Mock OrganizationRepository organizationRepository;
  @InjectMocks FindChurchOfOrganizationQueryHandlerImpl handler;

  @Test
  void returnsTheRootOfTheTree() {
    var node = PartyId.generate();
    var church = UUID.randomUUID();
    when(organizationRepository.findChurchIdOf(node.value())).thenReturn(Optional.of(church));

    assertThat(handler.handle(new FindChurchOfOrganizationQuery(node)))
        .contains(new FindChurchOfOrganizationResult(new PartyId(church)));
  }
}
