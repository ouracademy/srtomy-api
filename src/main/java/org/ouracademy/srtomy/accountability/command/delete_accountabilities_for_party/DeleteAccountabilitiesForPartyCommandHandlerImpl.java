package org.ouracademy.srtomy.accountability.command.delete_accountabilities_for_party;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteAccountabilitiesForPartyCommandHandlerImpl
    implements DeleteAccountabilitiesForPartyCommandHandler {

  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional
  public DeleteAccountabilitiesForPartyResult handle(
      DeleteAccountabilitiesForPartyCommand command) {
    accountabilityRepository.deleteAllByParentId(command.partyId());
    accountabilityRepository.deleteAllByChildId(command.partyId());
    return new DeleteAccountabilitiesForPartyResult();
  }
}
