package org.ouracademy.ovejafiel.event.command.set_registration_check_in;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetRegistrationCheckInCommandHandlerImpl implements SetRegistrationCheckInCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;

  @Override
  @Transactional
  public CheckInResult handle(SetRegistrationCheckInCommand command) {
    var registration =
        eventRegistrationRepository
            .findById(command.registrationId())
            .filter(found -> found.getEvent().getId().equals(command.eventId()))
            .orElseThrow(() -> new NoSuchElementException("Registration not found"));
    var already = registration.isCheckedIn();
    if (command.checkedIn()) {
      registration.checkIn(CheckInSource.MANUAL);
    } else {
      registration.undoCheckIn();
    }
    return new CheckInResult(
        registration.getId(),
        registration.getUser() == null ? null : registration.getUser().getId(),
        registration.getFullName(),
        registration.getTicketType().getName(),
        already && command.checkedIn(),
        registration.getCheckedInAt());
  }
}
