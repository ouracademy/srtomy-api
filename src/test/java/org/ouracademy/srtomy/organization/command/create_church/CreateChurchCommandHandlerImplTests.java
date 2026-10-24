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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommand;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommandHandler;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionResult;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommand;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommandHandler;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.command.create_church.CreateChurchCommand.Creator;
import org.ouracademy.srtomy.organization.command.create_church.CreateChurchCommand.PositionSeed;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonCommand;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonResult;
import org.ouracademy.srtomy.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.srtomy.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserResult;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.query.UserResult;

@ExtendWith(MockitoExtension.class)
class CreateChurchCommandHandlerImplTests {

  static final OrganizationInfo CHURCH_INFO = new OrganizationInfo("Iglesia", null, null, null);
  static final OrganizationInfo LOCATION_INFO = new OrganizationInfo("Sede", null, null, null);
  static final Creator CREATOR = new Creator("Ana", "Diaz", "Pastor");

  @Mock OrganizationWriter organizationWriter;
  @Mock SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Mock CreatePositionCommandHandler createPositionCommandHandler;
  @Mock GrantRoleCommandHandler grantRoleCommandHandler;
  @Mock CreatePersonCommandHandler createPersonCommandHandler;
  @Mock LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;

  CreateChurchCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateChurchCommandHandlerImpl(
            organizationWriter,
            syncCurrentUserCommandHandler,
            createPositionCommandHandler,
            grantRoleCommandHandler,
            createPersonCommandHandler,
            linkUserToPersonCommandHandler);
  }

  Organization church;
  Organization location;

  void givenUserAndTree(UserId userId) {
    church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    location = organization(PartyId.generate(), "Sede", Organization.Type.LOCATION, church);
    when(syncCurrentUserCommandHandler.handle(any(SyncCurrentUserCommand.class)))
        .thenReturn(
            new SyncCurrentUserResult(new UserResult(userId, "auth0|1", "ana@x.org", true), true));
    when(organizationWriter.create(eq(CHURCH_INFO), eq(Organization.Type.CHURCH), isNull()))
        .thenReturn(church);
    when(organizationWriter.create(LOCATION_INFO, Organization.Type.LOCATION, church.getId()))
        .thenReturn(location);
  }

  void givenPositionsAreCreated() {
    when(createPositionCommandHandler.handle(any()))
        .thenAnswer(
            call -> {
              CreatePositionCommand command = call.getArgument(0);
              return new CreatePositionResult(
                  new PositionResult(
                      AccountabilityTypeId.generate(),
                      command.name(),
                      command.churchId(),
                      command.roleIds(),
                      command.permissions()));
            });
  }

  CreateChurchCommand command(List<PositionSeed> positions, Creator creator) {
    return new CreateChurchCommand(
        CHURCH_INFO, LOCATION_INFO, positions, creator, "auth0|1", "ana@x.org", true);
  }

  @Test
  void ownerComesFirstThenPositionsWithTheirLevelThenTheCreatorsPerson() {
    var userId = UserId.generate();
    givenUserAndTree(userId);
    givenPositionsAreCreated();
    var personId = PartyId.generate();
    when(createPersonCommandHandler.handle(any())).thenReturn(new CreatePersonResult(personId));

    var result =
        handler.handle(
            command(
                List.of(
                    new PositionSeed("Pastor", Set.of(PredefinedRoles.ADMIN), Set.of()),
                    new PositionSeed(" pastor ", Set.of(), Set.of()),
                    new PositionSeed("", Set.of(), Set.of()),
                    new PositionSeed("Ujier", Set.of(), Set.of("attendance.write"))),
                CREATOR));

    assertThat(result.organization().id()).isEqualTo(church.getId());
    assertThat(result.location().parentId()).isEqualTo(church.getId());
    var order =
        inOrder(grantRoleCommandHandler, createPositionCommandHandler, createPersonCommandHandler);
    order
        .verify(grantRoleCommandHandler)
        .handle(new GrantRoleCommand(userId, PredefinedRoles.OWNER, church.getId(), null));
    order
        .verify(createPositionCommandHandler)
        .handle(
            new CreatePositionCommand(
                "auth0|1", church.getId(), "Pastor", Set.of(PredefinedRoles.ADMIN), Set.of()));
    order
        .verify(createPositionCommandHandler)
        .handle(
            new CreatePositionCommand(
                "auth0|1", church.getId(), "Ujier", Set.of(), Set.of("attendance.write")));
    verify(createPositionCommandHandler, times(2)).handle(any());

    var created = ArgumentCaptor.forClass(CreatePersonCommand.class);
    order.verify(createPersonCommandHandler).handle(created.capture());
    assertThat(created.getValue().subject()).isEqualTo("auth0|1");
    assertThat(created.getValue().info().firstName()).isEqualTo("Ana");
    assertThat(created.getValue().info().lastName()).isEqualTo("Diaz");
    assertThat(created.getValue().organizationId()).isEqualTo(location.getId());
    assertThat(created.getValue().positionIds()).hasSize(1);
    verify(linkUserToPersonCommandHandler)
        .handle(new LinkUserToPersonCommand(userId, personId, church.getId()));
  }

  @Test
  void theCreatorsPositionThatIsNotInTheListIsCreatedAsALabel() {
    givenUserAndTree(UserId.generate());
    givenPositionsAreCreated();
    when(createPersonCommandHandler.handle(any()))
        .thenReturn(new CreatePersonResult(PartyId.generate()));

    handler.handle(
        command(
            List.of(new PositionSeed("Ujier", Set.of(), Set.of("attendance.write"))),
            new Creator("Ana", "Diaz", "Hermano mayor")));

    verify(createPositionCommandHandler)
        .handle(
            new CreatePositionCommand(
                "auth0|1", church.getId(), "Hermano mayor", Set.of(), Set.of()));
  }

  @Test
  void aCreatorWithoutPositionHoldsNone() {
    givenUserAndTree(UserId.generate());
    when(createPersonCommandHandler.handle(any()))
        .thenReturn(new CreatePersonResult(PartyId.generate()));

    handler.handle(command(List.of(), new Creator("Ana", "Diaz", null)));

    verify(createPositionCommandHandler, times(0)).handle(any());
    var created = ArgumentCaptor.forClass(CreatePersonCommand.class);
    verify(createPersonCommandHandler).handle(created.capture());
    assertThat(created.getValue().positionIds()).isEqualTo(Set.of());
  }
}
