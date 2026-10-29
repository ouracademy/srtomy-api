package org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class EndAccountabilitiesOfChildCommandHandlerImpl
    implements EndAccountabilitiesOfChildCommandHandler {

  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional
  public EndAccountabilitiesOfChildResult handle(EndAccountabilitiesOfChildCommand command) {
    if (command.parentIds().isEmpty()) {
      return new EndAccountabilitiesOfChildResult(0);
    }
    var ended =
        accountabilityRepository.endByChildIdAndParentIdIn(
            command.childId(), command.parentIds(), LocalDateTime.now());
    return new EndAccountabilitiesOfChildResult(ended);
  }
}
