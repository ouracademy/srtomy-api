package org.ouracademy.ovejafiel.accountability.command.rename_position;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.PositionNames;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
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
