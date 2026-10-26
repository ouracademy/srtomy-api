package org.ouracademy.ovejafiel.organization.command.create_church;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.create_position.CreatePositionCommand;
import org.ouracademy.ovejafiel.accountability.command.create_position.CreatePositionCommandHandler;
import org.ouracademy.ovejafiel.iam.command.grant_role.GrantRoleCommand;
import org.ouracademy.ovejafiel.iam.command.grant_role.GrantRoleCommandHandler;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.organization.command.OrganizationWriter;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommand.PositionSeed;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommand;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateChurchCommandHandlerImpl implements CreateChurchCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  private final CreatePositionCommandHandler createPositionCommandHandler;
  private final GrantRoleCommandHandler grantRoleCommandHandler;
  private final CreatePersonCommandHandler createPersonCommandHandler;
  private final LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;

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

    // Owner first: creating positions with access is checked against what the creator holds.
    grantRoleCommandHandler.handle(
        new GrantRoleCommand(user.id(), PredefinedRoles.OWNER, church.getId(), null));

    // Repeated names (ignoring case and blanks around) are created once instead of failing.
    var seeds = new LinkedHashMap<String, PositionSeed>();
    command.positions().stream()
        .map(
            seed ->
                new PositionSeed(
                    seed.name() == null ? "" : seed.name().trim(),
                    seed.roleIds(),
                    seed.permissions()))
        .filter(seed -> !seed.name().isEmpty())
        .forEach(seed -> seeds.putIfAbsent(seed.name().toLowerCase(Locale.ROOT), seed));

    var creator = command.creator();
    var creatorPositionName = creator.positionName() == null ? "" : creator.positionName().trim();
    if (!creatorPositionName.isEmpty()) {
      // A position the creator names that is not in the list is still created, as a plain label.
      seeds.putIfAbsent(
          creatorPositionName.toLowerCase(Locale.ROOT),
          new PositionSeed(creatorPositionName, Set.of(), Set.of()));
    }

    var created =
        new LinkedHashMap<
            String, org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId>();
    seeds
        .values()
        .forEach(
            seed ->
                created.put(
                    seed.name().toLowerCase(Locale.ROOT),
                    createPositionCommandHandler
                        .handle(
                            new CreatePositionCommand(
                                command.subject(),
                                church.getId(),
                                seed.name(),
                                seed.roleIds(),
                                seed.permissions()))
                        .position()
                        .id()));

    var creatorPositions =
        creatorPositionName.isEmpty()
            ? Set.<org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId>of()
            : Set.of(created.get(creatorPositionName.toLowerCase(Locale.ROOT)));
    var person =
        createPersonCommandHandler.handle(
            new CreatePersonCommand(
                command.subject(),
                new PersonInfo(
                    creator.firstName(), creator.lastName(), null, null, null, null, null),
                location.getId(),
                creatorPositions));
    linkUserToPersonCommandHandler.handle(
        new LinkUserToPersonCommand(user.id(), person.id(), church.getId()));

    return new CreateChurchResult(
        FindOrganizationResult.from(church), FindOrganizationResult.from(location));
  }
}
