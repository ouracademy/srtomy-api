package org.ouracademy.srtomy.organization.command.create_location;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateLocationCommandHandlerImpl implements CreateLocationCommandHandler {

  private final OrganizationWriter organizationWriter;

  @Override
  @Transactional
  public CreateLocationResult handle(CreateLocationCommand command) {
    var location =
        organizationWriter.create(command.info(), Organization.Type.LOCATION, command.parentId());
    return new CreateLocationResult(FindOrganizationResult.from(location));
  }
}
