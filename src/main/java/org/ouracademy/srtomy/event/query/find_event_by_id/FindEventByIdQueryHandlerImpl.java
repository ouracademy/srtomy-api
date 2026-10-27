package org.ouracademy.srtomy.event.query.find_event_by_id;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.query.FindEventResult;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindEventByIdQueryHandlerImpl implements FindEventByIdQueryHandler {

  private final EventRepository eventRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<FindEventResult> handle(FindEventByIdQuery query) {
    return eventRepository.findById(query.id()).map(FindEventResult::from);
  }
}
