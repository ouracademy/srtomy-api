package org.ouracademy.srtomy.accountability.query.find_position_by_id;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
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
