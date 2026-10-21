package org.ouracademy.srtomy.organization.command;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.DeleteAccountabilitiesForPartyCommand;
import org.ouracademy.srtomy.accountability.command.DeleteAccountabilitiesForPartyCommandHandler;
import org.ouracademy.srtomy.organization.command.DeleteOrganizationResult.Outcome;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteOrganizationCommandHandlerImpl implements DeleteOrganizationCommandHandler {

  private final OrganizationRepository organizationRepository;
  private final DeleteAccountabilitiesForPartyCommandHandler
      deleteAccountabilitiesForPartyCommandHandler;

  @Override
  @Transactional
  public DeleteOrganizationResult handle(DeleteOrganizationCommand command) {
    var id = command.id();
    return organizationRepository
        .findById(id)
        .map(
            organization -> {
              if (!organization.isDeletable()) {
                return new DeleteOrganizationResult(Outcome.NOT_DELETABLE);
              }
              deleteAccountabilitiesForPartyCommandHandler.handle(
                  new DeleteAccountabilitiesForPartyCommand(id));
              organizationRepository.deleteById(id);
              return new DeleteOrganizationResult(Outcome.DELETED);
            })
        .orElseGet(() -> new DeleteOrganizationResult(Outcome.NOT_FOUND));
  }
}
