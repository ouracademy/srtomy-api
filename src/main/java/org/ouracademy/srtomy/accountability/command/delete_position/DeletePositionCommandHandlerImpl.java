package org.ouracademy.srtomy.accountability.command.delete_position;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeletePositionCommandHandlerImpl implements DeletePositionCommandHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional
  public DeletePositionResult handle(DeletePositionCommand command) {
    var position =
        accountabilityTypeRepository
            .findById(command.positionId())
            .filter(AccountabilityType::isPosition);
    position.ifPresent(
        type -> {
          accountabilityRepository.deleteAllByType(type);
          accountabilityTypeRepository.delete(type);
        });
    return new DeletePositionResult(position.isPresent());
  }
}
