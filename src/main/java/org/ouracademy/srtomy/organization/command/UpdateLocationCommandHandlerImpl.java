package org.ouracademy.srtomy.organization.command;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.SyncOrganizationRolesCommand;
import org.ouracademy.srtomy.accountability.command.SyncOrganizationRolesCommandHandler;
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
