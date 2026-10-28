package org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.EffectiveRegistration;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CheckInRegistrationByTicketCommandHandlerImpl
    implements CheckInRegistrationByTicketCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventRepository eventRepository;
  private final EffectiveRegistration effectiveRegistration;

  @Override
  @Transactional
  public CheckInResult handle(CheckInRegistrationByTicketCommand command) {
    var found =
        eventRegistrationRepository.findByTicketCode(
            command.ticketCode() == null ? "" : command.ticketCode().trim());
    var registration =
        found
            .filter(
                candidate ->
                    candidate.getEvent().getId().equals(command.eventId())
                        || isValidThroughContainer(candidate, command.eventId()))
            .orElseThrow(() -> new NoSuchElementException("Ticket not found"));
    if (!registration.getEvent().getId().equals(command.eventId())) {
      return inheritedCheckIn(registration);
    }
    var already = registration.isCheckedIn();
    registration.checkIn(CheckInSource.QR_TICKET);
    return new CheckInResult(
        registration.getId(),
        registration.getUser() == null ? null : registration.getUser().getId(),
        registration.getFullName(),
        registration.getTicketType().getName(),
        already,
        registration.getCheckedInAt());
  }

  /**
   * The ticket belongs to another event: it only works when that one is the container of the
   * scanned sub-event and the sub-event inherits its registration. A sub-event with its own
   * registration, or an open one, answers 400 instead of pretending the ticket does not exist.
   */
  private boolean isValidThroughContainer(EventRegistration registration, EventId scannedId) {
    var scanned = eventRepository.findById(scannedId).filter(Event::isChild);
    if (scanned.isEmpty()
        || !scanned.get().getParent().getId().equals(registration.getEvent().getId())) {
      return false;
    }
    if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
      throw new DomainValidationException("La inscripción no está confirmada");
    }
    if (!effectiveRegistration
        .governing(scanned.get())
        .filter(governing -> governing.getId().equals(registration.getEvent().getId()))
        .isPresent()) {
      throw new DomainValidationException(
          "Este sub-evento no acepta el ticket del evento: tiene su propia inscripción o es libre");
    }
    return true;
  }

  /** The sign-up is the container's: the check-in is recorded in the sub-event's attendance. */
  private CheckInResult inheritedCheckIn(EventRegistration registration) {
    return new CheckInResult(
        registration.getId(),
        registration.getUser() == null ? null : registration.getUser().getId(),
        registration.getFullName(),
        registration.getTicketType().getName(),
        false,
        null,
        true);
  }
}
