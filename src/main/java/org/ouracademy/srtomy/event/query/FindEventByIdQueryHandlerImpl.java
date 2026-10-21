package org.ouracademy.srtomy.event.query;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.repository.SingleEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindEventByIdQueryHandlerImpl implements FindEventByIdQueryHandler {

  private final SingleEventRepository singleEventRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<FindEventResult> handle(FindEventByIdQuery query) {
    return singleEventRepository.findById(query.id()).map(FindEventResult::from);
  }
}
