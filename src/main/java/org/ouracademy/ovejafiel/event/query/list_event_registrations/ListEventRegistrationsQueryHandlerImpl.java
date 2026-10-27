package org.ouracademy.ovejafiel.event.query.list_event_registrations;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListEventRegistrationsQueryHandlerImpl implements ListEventRegistrationsQueryHandler {

  private final EventRegistrationRepository eventRegistrationRepository;

  @Override
  @Transactional(readOnly = true)
  public List<EventRegistrationResult> handle(ListEventRegistrationsQuery query) {
    return eventRegistrationRepository
        .findByEventIdAndStatusNotOrderByCreateDate(query.eventId(), RegistrationStatus.CANCELLED)
        .stream()
        .map(
            registration ->
                new EventRegistrationResult(
                    registration.getId(),
                    registration.getFirstName(),
                    registration.getLastName(),
                    registration.getEmail(),
                    registration.getPhoneNumber(),
                    registration.getUser() != null,
                    registration.getTicketType().getName(),
                    registration.getStatus(),
                    registration.getCreateDate(),
                    registration.getCheckedInAt(),
                    registration.getCheckInSource()))
        .toList();
  }
}
