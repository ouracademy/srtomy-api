package org.ouracademy.ovejafiel.event.query.find_event_organization;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindEventOrganizationQueryHandlerImpl implements FindEventOrganizationQueryHandler {

  private final EventRepository eventRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<EventOrganizationResult> handle(FindEventOrganizationQuery query) {
    return eventRepository
        .findById(query.eventId())
        .map(event -> new EventOrganizationResult(event.getOrganization().getId()));
  }
}
