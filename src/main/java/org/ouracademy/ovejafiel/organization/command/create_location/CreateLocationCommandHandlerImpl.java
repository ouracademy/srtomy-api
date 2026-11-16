package org.ouracademy.ovejafiel.organization.command.create_location;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.query.check_capacity.CheckCapacityQuery;
import org.ouracademy.ovejafiel.billing.query.check_capacity.CheckCapacityQueryHandler;
import org.ouracademy.ovejafiel.organization.command.OrganizationWriter;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateLocationCommandHandlerImpl implements CreateLocationCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final CheckCapacityQueryHandler checkCapacityQueryHandler;

  @Override
  @Transactional
  public CreateLocationResult handle(CreateLocationCommand command) {
    // The free plan has a maximum of locations (402 when it is reached).
    checkCapacityQueryHandler.handle(
        new CheckCapacityQuery(command.parentId(), CheckCapacityQuery.Resource.LOCATIONS));
    var location =
        organizationWriter.create(command.info(), Organization.Type.LOCATION, command.parentId());
    return new CreateLocationResult(FindOrganizationResult.from(location));
  }
}
