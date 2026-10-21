package org.ouracademy.srtomy.organization.command.update_location;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.sync_organization_roles.SyncOrganizationRolesCommand;
import org.ouracademy.srtomy.accountability.command.sync_organization_roles.SyncOrganizationRolesCommandHandler;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UpdateLocationCommandHandlerImpl implements UpdateLocationCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final SyncOrganizationRolesCommandHandler syncOrganizationRolesCommandHandler;

  @Override
  @Transactional
  public Optional<UpdateLocationResult> handle(UpdateLocationCommand command) {
    return organizationWriter
        .editInfo(command.id(), command.info())
        .map(
            location -> {
              syncOrganizationRolesCommandHandler.handle(
                  new SyncOrganizationRolesCommand(location.getId(), command.roleNames()));
              return new UpdateLocationResult(FindOrganizationResult.from(location));
            });
  }
}
