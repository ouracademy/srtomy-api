package org.ouracademy.srtomy.organization.command.update_location_child;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommand;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommandHandler;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.command.delete_accountabilities_by_parent_and_types.DeleteAccountabilitiesByParentAndTypesCommand;
import org.ouracademy.srtomy.accountability.command.delete_accountabilities_by_parent_and_types.DeleteAccountabilitiesByParentAndTypesCommandHandler;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UpdateLocationChildCommandHandlerImpl implements UpdateLocationChildCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final DeleteAccountabilitiesByParentAndTypesCommandHandler
      deleteAccountabilitiesByParentAndTypesCommandHandler;
  private final CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

  @Override
  @Transactional
  public Optional<UpdateLocationChildResult> handle(UpdateLocationChildCommand command) {
    return organizationWriter
        .editChild(command.id(), command.info(), command.type())
        .map(
            child -> {
              var childId = child.getId();
              // Only the group's members and leaders are replaced. The IS_PART_OF link from the
              // group to its location has the group as child, so it is not touched.
              deleteAccountabilitiesByParentAndTypesCommandHandler.handle(
                  new DeleteAccountabilitiesByParentAndTypesCommand(
                      childId, List.of(IS_PART_OF, LEADER)));
              var accountabilities =
                  Stream.concat(
                          command.memberIds().stream()
                              .map(id -> new CreateAccountabilityCommand(id, childId, IS_PART_OF)),
                          command.leaderIds().stream()
                              .map(id -> new CreateAccountabilityCommand(id, childId, LEADER)))
                      .toList();
              createAccountabilitiesCommandHandler.handle(
                  new CreateAccountabilitiesCommand(accountabilities));
              return new UpdateLocationChildResult(FindOrganizationResult.from(child));
            });
  }
}
