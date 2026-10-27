package org.ouracademy.ovejafiel.event.query.get_shared_event;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.query.EventAccess;
import org.ouracademy.ovejafiel.event.query.RecurrenceResult;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetSharedEventQueryHandlerImpl implements GetSharedEventQueryHandler {

  private final EventRepository eventRepository;
  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventAccess eventAccess;

  @Override
  @Transactional(readOnly = true)
  public Optional<GetSharedEventResult> handle(GetSharedEventQuery query) {
    return eventRepository
        .findByShareSlug(query.slug())
        .filter(event -> canOpen(query.subject(), event))
        .map(event -> result(query.subject(), event));
  }

  private boolean canOpen(String subject, Event event) {
    return event.getGeneralAccess() == GeneralAccess.ANYONE_WITH_LINK
        || eventAccess.levelOn(subject, event).atLeast(AccessLevel.VIEWER);
  }

  private GetSharedEventResult result(String subject, Event event) {
    var user = eventAccess.verifiedUser(subject);
    return new GetSharedEventResult(
        event.getTitle(),
        event.getDescription(),
        event.getAddress(),
        event.getStart(),
        event.getEnd(),
        Boolean.TRUE.equals(event.getIsACourse()),
        event.getOrganization().getName(),
        RecurrenceResult.from(event.getSeries()),
        eventAccess.canRegister(subject, event),
        user != null
            && eventRegistrationRepository.existsByEventIdAndEmailIgnoreCase(
                event.getId(), user.email()));
  }
}
