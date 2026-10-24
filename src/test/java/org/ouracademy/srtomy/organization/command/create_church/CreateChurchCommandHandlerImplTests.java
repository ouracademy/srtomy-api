package org.ouracademy.srtomy.organization.command.create_church;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommand;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommandHandler;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommand;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommandHandler;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserResult;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.query.UserResult;

@ExtendWith(MockitoExtension.class)
class CreateChurchCommandHandlerImplTests {

  static final OrganizationInfo CHURCH_INFO = new OrganizationInfo("Iglesia", null, null, null);
  static final OrganizationInfo LOCATION_INFO = new OrganizationInfo("Sede", null, null, null);

  @Mock OrganizationWriter organizationWriter;
  @Mock SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Mock CreatePositionCommandHandler createPositionCommandHandler;
  @Mock GrantRoleCommandHandler grantRoleCommandHandler;

  CreateChurchCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateChurchCommandHandlerImpl(
            organizationWriter,
            syncCurrentUserCommandHandler,
            createPositionCommandHandler,
            grantRoleCommandHandler);
  }

  @Test
  void handleCreatesTheChurchItsLocationAndPositionsAndMakesTheUserItsOwner() {
    var userId = UserId.generate();
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    var location = organization(PartyId.generate(), "Sede", Organization.Type.LOCATION, church);
    when(syncCurrentUserCommandHandler.handle(
            new SyncCurrentUserCommand("auth0|1", "ana@x.org", true)))
        .thenReturn(
            new SyncCurrentUserResult(new UserResult(userId, "auth0|1", "ana@x.org", true), true));
    when(organizationWriter.create(CHURCH_INFO, Organization.Type.CHURCH, null)).thenReturn(church);
    when(organizationWriter.create(LOCATION_INFO, Organization.Type.LOCATION, church.getId()))
        .thenReturn(location);

    var result =
        handler.handle(
            new CreateChurchCommand(
                CHURCH_INFO,
                LOCATION_INFO,
                List.of("Pastor", " pastor ", "", "Ujier"),
                "auth0|1",
                "ana@x.org",
                true));

    assertThat(result.organization().id()).isEqualTo(church.getId());
    assertThat(result.location().parentId()).isEqualTo(church.getId());
    var order = inOrder(createPositionCommandHandler, grantRoleCommandHandler);
    order
        .verify(createPositionCommandHandler)
        .handle(new CreatePositionCommand(church.getId(), "Pastor"));
    order
        .verify(createPositionCommandHandler)
        .handle(new CreatePositionCommand(church.getId(), "Ujier"));
    order
        .verify(grantRoleCommandHandler)
        .handle(new GrantRoleCommand(userId, PredefinedRoles.OWNER, church.getId(), null));
    verify(createPositionCommandHandler, times(2)).handle(any());
  }

  @Test
  void handleWithoutPositionsCreatesNone() {
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    when(syncCurrentUserCommandHandler.handle(any()))
        .thenReturn(
            new SyncCurrentUserResult(new UserResult(UserId.generate(), "s", null, false), false));
    when(organizationWriter.create(eq(CHURCH_INFO), eq(Organization.Type.CHURCH), isNull()))
        .thenReturn(church);
    when(organizationWriter.create(LOCATION_INFO, Organization.Type.LOCATION, church.getId()))
        .thenReturn(organization(PartyId.generate(), "Sede", Organization.Type.LOCATION, church));

    handler.handle(
        new CreateChurchCommand(CHURCH_INFO, LOCATION_INFO, List.of(), "s", null, false));

    verify(createPositionCommandHandler, times(0)).handle(any());
  }
}
