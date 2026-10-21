package org.ouracademy.srtomy.organization.command;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class EditOrganizationCommandHandlerImpl implements EditOrganizationCommandHandler {

  private final OrganizationWriter organizationWriter;

  @Override
  @Transactional
  public Optional<EditOrganizationResult> handle(EditOrganizationCommand command) {
    return organizationWriter
        .editInfo(command.id(), command.info())
        .map(organization -> new EditOrganizationResult(FindOrganizationResult.from(organization)));
  }
}
