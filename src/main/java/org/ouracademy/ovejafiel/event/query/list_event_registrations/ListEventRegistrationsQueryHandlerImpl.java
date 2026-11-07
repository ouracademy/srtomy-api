package org.ouracademy.ovejafiel.event.query.list_event_registrations;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.InheritedCheckIns;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListEventRegistrationsQueryHandlerImpl implements ListEventRegistrationsQueryHandler {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventRepository eventRepository;
  private final InheritedCheckIns inheritedCheckIns;

  /**
   * The sign-ups of the event. For a sub-event that inherits them, those of its mega event, each
   * with the entry recorded in this sub-event.
   */
  @Override
  @Transactional(readOnly = true)
  public List<EventRegistrationResult> handle(ListEventRegistrationsQuery query) {
    var inherited = eventRepository.findById(query.eventId()).filter(inheritedCheckIns::inherits);
    if (inherited.isPresent()) {
      var subEvent = inherited.get();
      var entries = inheritedCheckIns.of(subEvent);
      return eventRegistrationRepository
          .findByEventIdAndStatusNotOrderByCreateDate(
              subEvent.getParent().getId(), RegistrationStatus.CANCELLED)
          .stream()
          // Only the tickets that give access to this sub-event are expected in it.
          .filter(registration -> registration.getTicketType().grants(subEvent))
          .map(
              registration -> {
                var entry = entries.get(registration.getId());
                return result(
                    registration,
                    entry == null ? null : entry.getCheckedInAt(),
                    entry == null ? null : entry.getCheckInSource());
              })
          .toList();
    }
    return eventRegistrationRepository
        .findByEventIdAndStatusNotOrderByCreateDate(query.eventId(), RegistrationStatus.CANCELLED)
        .stream()
        .map(
            registration ->
                result(
                    registration, registration.getCheckedInAt(), registration.getCheckInSource()))
        .toList();
  }

  private EventRegistrationResult result(
      EventRegistration registration,
      java.time.LocalDateTime checkedInAt,
      org.ouracademy.ovejafiel.event.domain.CheckInSource source) {
    return new EventRegistrationResult(
        registration.getId(),
        registration.getFirstName(),
        registration.getLastName(),
        registration.getEmail(),
        registration.getPhoneNumber(),
        registration.getUser() != null,
        registration.getTicketType().getName(),
        registration.getStatus(),
        registration.getCreateDate(),
        checkedInAt,
        source);
  }
}
