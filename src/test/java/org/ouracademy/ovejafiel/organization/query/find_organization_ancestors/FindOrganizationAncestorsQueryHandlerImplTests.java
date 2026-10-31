package org.ouracademy.ovejafiel.organization.query.find_organization_ancestors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.organization.repository.OrganizationRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class FindOrganizationAncestorsQueryHandlerImplTests {

  OrganizationRepository repository = mock(OrganizationRepository.class);
  FindOrganizationAncestorsQueryHandlerImpl handler =
      new FindOrganizationAncestorsQueryHandlerImpl(repository);

  @Test
  void returnsTheNodesAboveTheChurchFirst() {
    var church = PartyId.generate();
    var location = PartyId.generate();
    var group = PartyId.generate();
    when(repository.findAncestorIds(group.value()))
        .thenReturn(List.of(church.value(), location.value()));

    assertThat(handler.handle(new FindOrganizationAncestorsQuery(group)).ids())
        .containsExactly(church, location);
  }

  @Test
  void aChurchHasNoAncestors() {
    var church = PartyId.generate();
    when(repository.findAncestorIds(church.value())).thenReturn(List.of());

    assertThat(handler.handle(new FindOrganizationAncestorsQuery(church)).ids()).isEmpty();
  }
}
