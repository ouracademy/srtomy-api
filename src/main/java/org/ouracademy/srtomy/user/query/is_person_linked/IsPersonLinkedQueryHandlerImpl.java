package org.ouracademy.srtomy.user.query.is_person_linked;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.user.repository.UserPersonLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class IsPersonLinkedQueryHandlerImpl implements IsPersonLinkedQueryHandler {

  private final UserPersonLinkRepository userPersonLinkRepository;

  @Override
  @Transactional(readOnly = true)
  public IsPersonLinkedResult handle(IsPersonLinkedQuery query) {
    return new IsPersonLinkedResult(userPersonLinkRepository.existsByPersonId(query.personId()));
  }
}
