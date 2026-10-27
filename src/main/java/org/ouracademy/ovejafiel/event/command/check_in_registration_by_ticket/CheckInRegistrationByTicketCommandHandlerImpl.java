package org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CheckInRegistrationByTicketCommandHandlerImpl
    implements CheckInRegistrationByTicketCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;

  @Override
  @Transactional
  public CheckInResult handle(CheckInRegistrationByTicketCommand command) {
    var registration =
        eventRegistrationRepository
            .findByTicketCode(command.ticketCode() == null ? "" : command.ticketCode().trim())
            .filter(found -> found.getEvent().getId().equals(command.eventId()))
            .orElseThrow(() -> new NoSuchElementException("Ticket not found"));
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
}
