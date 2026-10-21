package org.ouracademy.srtomy.accountability.query;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetRolesOfPersonQueryHandlerImpl implements GetRolesOfPersonQueryHandler {

  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AccountabilityTypeResult> handle(GetRolesOfPersonQuery query) {
    return accountabilityRepository
        .findByParentIdAndChildId(query.locationId(), query.personId())
        .stream()
        .map(x -> x.getType())
        .filter(x -> x.getCategory() != null && Category.ROLE.equals(x.getCategory()))
        .map(AccountabilityTypeResult::from)
        .toList();
  }
}
