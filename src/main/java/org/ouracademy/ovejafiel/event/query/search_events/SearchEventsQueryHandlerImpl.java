package org.ouracademy.ovejafiel.event.query.search_events;

import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Events of the node and of everything below it; each carries its own organization. */
@Service
@RequiredArgsConstructor
class SearchEventsQueryHandlerImpl implements SearchEventsQueryHandler {

  private final EventRepository eventRepository;
  private final FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public List<FindEventResult> handle(SearchEventsQuery query) {
    var organizationIds =
        findOrganizationSubtreeQueryHandler
            .handle(new FindOrganizationSubtreeQuery(query.organizationId()))
            .ids();
    var events =
        eventRepository.searchEventsOfOrgIdsAndBetweenDates(
            organizationIds, query.start(), query.end());
    // One query for the size of every program, not one per event.
    var sizes = new HashMap<EventId, Integer>();
    if (!events.isEmpty()) {
      eventRepository
          .countSubEvents(events.stream().map(Event::getId).toList())
          .forEach(row -> sizes.put((EventId) row[0], ((Long) row[1]).intValue()));
    }
    return events.stream()
        .map(
            event ->
                FindEventResult.from(event).withProgramSize(sizes.getOrDefault(event.getId(), 0)))
        .toList();
  }
}
