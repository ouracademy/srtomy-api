package org.ouracademy.ovejafiel.accountability.query.find_position_by_id;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindPositionByIdQueryHandlerImpl implements FindPositionByIdQueryHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<PositionResult> handle(FindPositionByIdQuery query) {
    return accountabilityTypeRepository
        .findById(query.positionId())
        .filter(AccountabilityType::isPosition)
        .map(PositionResult::from);
  }
}
