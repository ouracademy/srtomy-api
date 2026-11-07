package org.ouracademy.ovejafiel.event.command.check_in_user_registration;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.InheritedCheckIns;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CheckInUserRegistrationCommandHandlerImpl implements CheckInUserRegistrationCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventRepository eventRepository;
  private final InheritedCheckIns inheritedCheckIns;

  @Override
  @Transactional
  public Optional<CheckInResult> handle(CheckInUserRegistrationCommand command) {
    var subEvent = eventRepository.findById(command.eventId()).filter(inheritedCheckIns::inherits);
    // A sub-event that inherits is entered with the sign-up of its mega event.
    var lookup = subEvent.map(found -> found.getParent().getId()).orElse(command.eventId());
    return find(command, lookup)
        .filter(found -> found.getStatus() == RegistrationStatus.CONFIRMED)
        // A ticket that does not give access to this sub-event is as if they had none here.
        .filter(found -> subEvent.isEmpty() || found.getTicketType().grants(subEvent.get()))
        .map(
            registration -> {
              if (subEvent.isPresent()) {
                return inheritedResult(registration, subEvent.get());
              }
              var already = registration.isCheckedIn();
              registration.checkIn(CheckInSource.QR_EVENT);
              return new CheckInResult(
                  registration.getId(),
                  registration.getUser() == null ? null : registration.getUser().getId(),
                  registration.getFullName(),
                  registration.getTicketType().getName(),
                  already,
                  registration.getCheckedInAt());
            });
  }

  private CheckInResult inheritedResult(EventRegistration registration, Event subEvent) {
    var mark = inheritedCheckIns.mark(registration, subEvent, CheckInSource.QR_EVENT);
    return new CheckInResult(
        registration.getId(),
        registration.getUser() == null ? null : registration.getUser().getId(),
        registration.getFullName(),
        registration.getTicketType().getName(),
        mark.already(),
        mark.checkedInAt(),
        true);
  }

  private Optional<EventRegistration> find(
      CheckInUserRegistrationCommand command, EventId eventId) {
    var byUser =
        eventRegistrationRepository.findFirstByEventIdAndUserIdAndStatusNot(
            eventId, command.userId(), RegistrationStatus.CANCELLED);
    if (byUser.isPresent() || command.email() == null) {
      return byUser;
    }
    return eventRegistrationRepository.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(
        eventId, command.email(), RegistrationStatus.CANCELLED);
  }
}
