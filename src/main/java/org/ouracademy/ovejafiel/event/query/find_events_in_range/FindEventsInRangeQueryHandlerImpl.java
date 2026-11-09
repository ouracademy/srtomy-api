package org.ouracademy.ovejafiel.event.query.find_events_in_range;

import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Events of already-resolved node ids; each carries its own organization. */
@Service
@RequiredArgsConstructor
class FindEventsInRangeQueryHandlerImpl implements FindEventsInRangeQueryHandler {

  private final EventRepository eventRepository;

  @Override
  @Transactional(readOnly = true)
  public List<FindEventResult> handle(FindEventsInRangeQuery query) {
    return eventRepository
        .searchEventsOfOrgIdsAndBetweenDates(
            query.organizationIds(),
            query.start().atZone(ZoneOffset.UTC),
            query.end().atZone(ZoneOffset.UTC))
        .stream()
        .map(FindEventResult::from)
        .toList();
  }
}
