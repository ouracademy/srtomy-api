package org.ouracademy.srtomy.organization.command;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.CreateAccountabilitiesCommand;
import org.ouracademy.srtomy.accountability.command.CreateAccountabilitiesCommandHandler;
import org.ouracademy.srtomy.accountability.command.CreateAccountabilityCommand;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
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
