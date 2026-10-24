package org.ouracademy.srtomy.event.query.get_event_sharing;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.query.EventSharingResult;
import org.ouracademy.srtomy.event.repository.EventRegistrationRepository;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.event.repository.EventShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetEventSharingQueryHandlerImpl implements GetEventSharingQueryHandler {

  private final EventRepository eventRepository;
  private final EventShareRepository eventShareRepository;
  private final EventRegistrationRepository eventRegistrationRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<EventSharingResult> handle(GetEventSharingQuery query) {
    return eventRepository
        .findById(query.eventId())
        .map(
            event ->
                EventSharingResult.of(
                    event,
                    eventShareRepository.findByEventIdOrderByCreateDate(event.getId()),
                    eventRegistrationRepository.countByEventId(event.getId())));
  }
}
