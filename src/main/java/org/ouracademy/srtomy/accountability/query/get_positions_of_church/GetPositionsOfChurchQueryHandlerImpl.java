package org.ouracademy.srtomy.accountability.query.get_positions_of_church;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPositionsOfChurchQueryHandlerImpl implements GetPositionsOfChurchQueryHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PositionResult> handle(GetPositionsOfChurchQuery query) {
    return accountabilityTypeRepository
        .findByPartyIdAndCategoryOrderByName(query.churchId(), Category.POSITION)
        .stream()
        .map(PositionResult::from)
        .toList();
  }
}
