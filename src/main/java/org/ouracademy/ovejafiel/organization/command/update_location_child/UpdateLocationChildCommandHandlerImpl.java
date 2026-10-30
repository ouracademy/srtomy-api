package org.ouracademy.ovejafiel.organization.command.update_location_child;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.command.OrganizationWriter;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UpdateLocationChildCommandHandlerImpl implements UpdateLocationChildCommandHandler {

  private final OrganizationWriter organizationWriter;

  @Override
  @Transactional
  public Optional<UpdateLocationChildResult> handle(UpdateLocationChildCommand command) {
    return organizationWriter
        .editChild(command.id(), command.info(), command.type())
        .map(child -> new UpdateLocationChildResult(FindOrganizationResult.from(child)));
  }
}
