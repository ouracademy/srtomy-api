package org.ouracademy.srtomy.organization.command.update_location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.sync_organization_roles.SyncOrganizationRolesCommand;
import org.ouracademy.srtomy.accountability.command.sync_organization_roles.SyncOrganizationRolesCommandHandler;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class UpdateLocationCommandHandlerImplTests {

  @Mock OrganizationWriter organizationWriter;
  @Mock SyncOrganizationRolesCommandHandler syncOrganizationRolesCommandHandler;

  UpdateLocationCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new UpdateLocationCommandHandlerImpl(
            organizationWriter, syncOrganizationRolesCommandHandler);
  }

  @Test
  void handleUpdatesTheInfoAndSyncsTheRoles() {
    var id = PartyId.generate();
    var info = new OrganizationInfo("Sede Norte", "Principal", "Av. Uno 123", "987654321");
    var roles = new TreeSet<>(Set.of("VOLUNTARIO"));
    when(organizationWriter.editInfo(id, info))
        .thenReturn(Optional.of(organization(id, "Sede Norte", Organization.Type.LOCATION)));

    var result = handler.handle(new UpdateLocationCommand(id, info, roles));

    assertThat(result).isPresent();
    assertThat(result.get().organization().name()).isEqualTo("Sede Norte");
    verify(syncOrganizationRolesCommandHandler).handle(new SyncOrganizationRolesCommand(id, roles));
  }

  @Test
  void handleWithNoRolesRemovesTheCustomRoles() {
    var id = PartyId.generate();
    var info = new OrganizationInfo("Sede Norte", null, "Av. Uno 123", null);
    when(organizationWriter.editInfo(id, info))
        .thenReturn(Optional.of(organization(id, "Sede Norte", Organization.Type.LOCATION)));

    handler.handle(new UpdateLocationCommand(id, info, new TreeSet<>()));

    verify(syncOrganizationRolesCommandHandler)
        .handle(new SyncOrganizationRolesCommand(id, new TreeSet<>()));
  }

  @Test
  void handleOfAnUnknownLocationReturnsEmptyAndSyncsNothing() {
    var id = PartyId.generate();
    var info = new OrganizationInfo("Sede Norte", null, null, null);
    when(organizationWriter.editInfo(id, info)).thenReturn(Optional.empty());

    var result = handler.handle(new UpdateLocationCommand(id, info, new TreeSet<>()));

    assertThat(result).isEmpty();
    verifyNoInteractions(syncOrganizationRolesCommandHandler);
  }
}
