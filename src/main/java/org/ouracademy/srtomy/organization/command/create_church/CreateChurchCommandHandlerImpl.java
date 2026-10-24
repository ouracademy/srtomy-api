package org.ouracademy.srtomy.organization.command.create_church;

import java.util.LinkedHashMap;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommand;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommandHandler;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommand;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommandHandler;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateChurchCommandHandlerImpl implements CreateChurchCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  private final CreatePositionCommandHandler createPositionCommandHandler;
  private final GrantRoleCommandHandler grantRoleCommandHandler;

  @Override
  @Transactional
  public CreateChurchResult handle(CreateChurchCommand command) {
    var user =
        syncCurrentUserCommandHandler
            .handle(
                new SyncCurrentUserCommand(
                    command.subject(), command.email(), command.emailVerified()))
            .user();
    var church = organizationWriter.create(command.info(), Organization.Type.CHURCH, null);
    var location =
        organizationWriter.create(command.location(), Organization.Type.LOCATION, church.getId());

    // Repeated names (ignoring case and blanks around) are created once instead of failing.
    var names = new LinkedHashMap<String, String>();
    command.positionNames().stream()
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .forEach(name -> names.putIfAbsent(name.toLowerCase(Locale.ROOT), name));
    names
        .values()
        .forEach(
            name ->
                createPositionCommandHandler.handle(
                    new CreatePositionCommand(church.getId(), name)));

    grantRoleCommandHandler.handle(
        new GrantRoleCommand(user.id(), PredefinedRoles.OWNER, church.getId(), null));
    return new CreateChurchResult(
        FindOrganizationResult.from(church), FindOrganizationResult.from(location));
  }
}
