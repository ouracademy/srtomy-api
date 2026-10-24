package org.ouracademy.srtomy.event.query.get_shared_event;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.domain.AccessLevel;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.GeneralAccess;
import org.ouracademy.srtomy.event.domain.RecurrentEvent;
import org.ouracademy.srtomy.event.query.EventAccess;
import org.ouracademy.srtomy.event.repository.EventRegistrationRepository;
import org.ouracademy.srtomy.event.repository.EventRepository;
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
    List<Integer> days = null;
    var endRecur = (java.time.LocalDate) null;
    if (event instanceof RecurrentEvent recurrent) {
      days = recurrent.getDaysOfWeek() == null ? null : List.copyOf(recurrent.getDaysOfWeek());
      endRecur = recurrent.getEndRecur();
    }
    return new GetSharedEventResult(
        event.getTitle(),
        event.getDescription(),
        event.getAddress(),
        event.getStart(),
        event.getEnd(),
        Boolean.TRUE.equals(event.getIsACourse()),
        event.getOrganization().getName(),
        days,
        endRecur,
        eventAccess.canRegister(subject, event),
        user != null
            && eventRegistrationRepository.existsByEventIdAndEmailIgnoreCase(
                event.getId(), user.email()));
  }
}
