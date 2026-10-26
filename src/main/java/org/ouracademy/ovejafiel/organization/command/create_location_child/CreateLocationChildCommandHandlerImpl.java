package org.ouracademy.ovejafiel.organization.command.create_location_child;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.create_accountabilities.CreateAccountabilitiesCommand;
import org.ouracademy.ovejafiel.accountability.command.create_accountabilities.CreateAccountabilitiesCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.ovejafiel.organization.command.OrganizationWriter;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateLocationChildCommandHandlerImpl implements CreateLocationChildCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

  @Override
  @Transactional
  public CreateLocationChildResult handle(CreateLocationChildCommand command) {
    var locationChild =
        organizationWriter.create(command.info(), command.type(), command.parentId());
    var childId = locationChild.getId();
    // Members and leaders are inserted in a single batch, members first.
    var accountabilities =
        Stream.concat(
                command.memberIds().stream()
                    .map(id -> new CreateAccountabilityCommand(id, childId, IS_PART_OF)),
                command.leaderIds().stream()
                    .map(id -> new CreateAccountabilityCommand(id, childId, LEADER)))
            .toList();
    createAccountabilitiesCommandHandler.handle(
        new CreateAccountabilitiesCommand(accountabilities));
    return new CreateLocationChildResult(FindOrganizationResult.from(locationChild));
  }
}
