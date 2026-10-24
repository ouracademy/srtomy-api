package org.ouracademy.srtomy.accountability.command.rename_position;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.PositionNames;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RenamePositionCommandHandlerImpl implements RenamePositionCommandHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final PositionNames positionNames;

  @Override
  @Transactional
  public Optional<RenamePositionResult> handle(RenamePositionCommand command) {
    return accountabilityTypeRepository
        .findById(command.positionId())
        .filter(AccountabilityType::isPosition)
        .map(
            position -> {
              var unchanged = position.getName().equalsIgnoreCase(command.name().trim());
              var name =
                  unchanged
                      ? command.name().trim()
                      : positionNames.requireAvailable(position.getParty().getId(), command.name());
              position.rename(name);
              return new RenamePositionResult(PositionResult.from(position));
            });
  }
}
