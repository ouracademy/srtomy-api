package org.ouracademy.ovejafiel.user.query.find_linked_person;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.user.repository.UserPersonLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindLinkedPersonQueryHandlerImpl implements FindLinkedPersonQueryHandler {

  private final UserPersonLinkRepository userPersonLinkRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<FindLinkedPersonResult> handle(FindLinkedPersonQuery query) {
    return userPersonLinkRepository
        .findByUserIdAndChurchId(query.userId(), query.churchId())
        .map(link -> new FindLinkedPersonResult(link.getPerson().getId()));
  }
}
