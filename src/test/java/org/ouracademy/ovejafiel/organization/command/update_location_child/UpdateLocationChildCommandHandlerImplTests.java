package org.ouracademy.ovejafiel.organization.command.update_location_child;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.organization.command.OrganizationWriter;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class UpdateLocationChildCommandHandlerImplTests {

  @Mock OrganizationWriter organizationWriter;

  UpdateLocationChildCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler = new UpdateLocationChildCommandHandlerImpl(organizationWriter);
  }

  @Test
  void handleUpdatesTheDataAndTheType() {
    var info = new OrganizationInfo("Jóvenes", "Nuevo", null, null);
    var groupId = PartyId.generate();
    when(organizationWriter.editChild(groupId, info, Organization.Type.MINISTRY))
        .thenReturn(Optional.of(organization(groupId, "Jóvenes", Organization.Type.MINISTRY)));

    var result =
        handler.handle(new UpdateLocationChildCommand(groupId, info, Organization.Type.MINISTRY));

    assertThat(result).isPresent();
    assertThat(result.get().organization().id()).isEqualTo(groupId);
  }

  @Test
  void handleReturnsEmptyWhenTheOrganizationIsNotAGroupOrMinistry() {
    var info = new OrganizationInfo("Sede", null, null, null);
    var id = PartyId.generate();
    when(organizationWriter.editChild(id, info, Organization.Type.GROUP))
        .thenReturn(Optional.empty());

    assertThat(handler.handle(new UpdateLocationChildCommand(id, info, Organization.Type.GROUP)))
        .isEmpty();
  }
}
