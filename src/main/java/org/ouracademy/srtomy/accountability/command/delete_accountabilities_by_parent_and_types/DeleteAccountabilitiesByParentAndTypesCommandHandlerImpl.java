package org.ouracademy.srtomy.accountability.command.delete_accountabilities_by_parent_and_types;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteAccountabilitiesByParentAndTypesCommandHandlerImpl
    implements DeleteAccountabilitiesByParentAndTypesCommandHandler {

  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional
  public DeleteAccountabilitiesByParentAndTypesResult handle(
      DeleteAccountabilitiesByParentAndTypesCommand command) {
    accountabilityRepository.deleteAll(
        accountabilityRepository.findByParentIdAndTypeIdIn(command.parentId(), command.typeIds()));
    return new DeleteAccountabilitiesByParentAndTypesResult();
  }
}
