package org.ouracademy.ovejafiel.organization.command.update_location;

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
class UpdateLocationCommandHandlerImplTests {

  @Mock OrganizationWriter organizationWriter;

  UpdateLocationCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler = new UpdateLocationCommandHandlerImpl(organizationWriter);
  }

  @Test
  void handleUpdatesTheInfo() {
    var id = PartyId.generate();
    var info = new OrganizationInfo("Sede Norte", "Principal", "Av. Uno 123", "987654321");
    when(organizationWriter.editInfo(id, info))
        .thenReturn(Optional.of(organization(id, "Sede Norte", Organization.Type.LOCATION)));

    var result = handler.handle(new UpdateLocationCommand(id, info));

    assertThat(result).isPresent();
    assertThat(result.get().organization().name()).isEqualTo("Sede Norte");
  }

  @Test
  void handleOfAnUnknownLocationReturnsEmpty() {
    var id = PartyId.generate();
    var info = new OrganizationInfo("Sede Norte", null, null, null);
    when(organizationWriter.editInfo(id, info)).thenReturn(Optional.empty());

    assertThat(handler.handle(new UpdateLocationCommand(id, info))).isEmpty();
  }
}
