package org.ouracademy.srtomy.organization.command;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.SyncOrganizationRolesCommand;
import org.ouracademy.srtomy.accountability.command.SyncOrganizationRolesCommandHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateLocationCommandHandlerImpl implements CreateLocationCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final SyncOrganizationRolesCommandHandler syncOrganizationRolesCommandHandler;

  @Override
  @Transactional
  public CreateLocationResult handle(CreateLocationCommand command) {
    var location =
        organizationWriter.create(command.info(), Organization.Type.LOCATION, command.parentId());
    syncOrganizationRolesCommandHandler.handle(
        new SyncOrganizationRolesCommand(location.getId(), command.roleNames()));
    return new CreateLocationResult(FindOrganizationResult.from(location));
  }
}
