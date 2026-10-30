package org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.MemberStateEvents;
import org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class EndAccountabilitiesOfChildCommandHandlerImpl
    implements EndAccountabilitiesOfChildCommandHandler {

  private final AccountabilityRepository accountabilityRepository;
  private final MemberStateEvents memberStateEvents;

  @Override
  @Transactional
  public EndAccountabilitiesOfChildResult handle(EndAccountabilitiesOfChildCommand command) {
    if (command.parentIds().isEmpty()) {
      return new EndAccountabilitiesOfChildResult(0);
    }
    // Leaving a node ends the state the person had there: that is what the history records.
    accountabilityRepository
        .findByChildIdAndParentIdInAndTypeIdIn(
            command.childId(),
            command.parentIds(),
            List.of(SystemAccountabilityTypes.MEMBER_PROGRESS_STATES))
        .forEach(
            state ->
                SystemAccountabilityTypes.progressStateName(state.getType().getId())
                    .ifPresent(
                        name ->
                            memberStateEvents.left(
                                command.childId(), state.getParent().getId(), name)));
    var ended =
        accountabilityRepository.endByChildIdAndParentIdIn(
            command.childId(), command.parentIds(), LocalDateTime.now());
    return new EndAccountabilitiesOfChildResult(ended);
  }
}
