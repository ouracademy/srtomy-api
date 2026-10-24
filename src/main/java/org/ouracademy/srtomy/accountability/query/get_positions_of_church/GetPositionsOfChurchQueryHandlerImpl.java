package org.ouracademy.srtomy.accountability.query.get_positions_of_church;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.query.PositionSummaryResult;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository.PositionHolders;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPositionsOfChurchQueryHandlerImpl implements GetPositionsOfChurchQueryHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PositionSummaryResult> handle(GetPositionsOfChurchQuery query) {
    var positions =
        accountabilityTypeRepository.findByPartyIdAndCategoryOrderByName(
            query.churchId(), Category.POSITION);
    if (positions.isEmpty()) {
      return List.of();
    }
    Map<AccountabilityTypeId, Long> holders =
        accountabilityRepository
            .countHoldersOf(positions.stream().map(AccountabilityType::getId).toList())
            .stream()
            .collect(Collectors.toMap(PositionHolders::getPositionId, PositionHolders::getHolders));
    return positions.stream()
        .map(
            position ->
                new PositionSummaryResult(
                    PositionResult.from(position), holders.getOrDefault(position.getId(), 0L)))
        .toList();
  }
}
