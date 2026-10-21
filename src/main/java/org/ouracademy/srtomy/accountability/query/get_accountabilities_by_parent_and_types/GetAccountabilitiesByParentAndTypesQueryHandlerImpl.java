package org.ouracademy.srtomy.accountability.query.get_accountabilities_by_parent_and_types;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetAccountabilitiesByParentAndTypesQueryHandlerImpl
    implements GetAccountabilitiesByParentAndTypesQueryHandler {

  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AccountabilityResult> handle(GetAccountabilitiesByParentAndTypesQuery query) {
    return accountabilityRepository
        .findByParentIdAndTypeIdIn(query.parentId(), query.typeIds())
        .stream()
        .map(AccountabilityResult::from)
        .toList();
  }
}
