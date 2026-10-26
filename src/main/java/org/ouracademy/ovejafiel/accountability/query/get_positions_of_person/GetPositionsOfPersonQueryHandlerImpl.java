package org.ouracademy.ovejafiel.accountability.query.get_positions_of_person;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPositionsOfPersonQueryHandlerImpl implements GetPositionsOfPersonQueryHandler {

  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PositionResult> handle(GetPositionsOfPersonQuery query) {
    return accountabilityRepository
        .findByParentIdAndChildId(query.organizationId(), query.personId())
        .stream()
        .map(Accountability::getType)
        .filter(AccountabilityType::isPosition)
        .map(PositionResult::from)
        .toList();
  }
}
