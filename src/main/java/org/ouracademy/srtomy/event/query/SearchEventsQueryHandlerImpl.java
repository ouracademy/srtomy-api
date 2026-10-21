package org.ouracademy.srtomy.event.query;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SearchEventsQueryHandlerImpl implements SearchEventsQueryHandler {

  private final EventRepository eventRepository;

  @Override
  @Transactional(readOnly = true)
  public List<FindEventResult> handle(SearchEventsQuery query) {
    return eventRepository
        .searchEventsOfOrgIdAndBetweenDates(query.organizationId(), query.start(), query.end())
        .stream()
        .map(FindEventResult::from)
        .toList();
  }
}
