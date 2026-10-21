package org.ouracademy.srtomy.accountability.query.get_predefined_roles;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.PASTOR;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.USHER;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.WORSHIP_LEADER;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPredefinedRolesQueryHandlerImpl implements GetPredefinedRolesQueryHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AccountabilityTypeResult> handle(GetPredefinedRolesQuery query) {
    return accountabilityTypeRepository.findAllById(List.of(PASTOR, WORSHIP_LEADER, USHER)).stream()
        .map(AccountabilityTypeResult::from)
        .toList();
  }
}
