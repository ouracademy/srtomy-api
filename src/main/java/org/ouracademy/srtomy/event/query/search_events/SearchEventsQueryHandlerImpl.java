package org.ouracademy.srtomy.event.query.search_events;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.query.FindEventResult;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.srtomy.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
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
    return eventRepository
        .searchEventsOfOrgIdsAndBetweenDates(organizationIds, query.start(), query.end())
        .stream()
        .map(FindEventResult::from)
        .toList();
  }
}
