package org.ouracademy.ovejafiel.event.command.set_registration_check_in;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.query.InheritedCheckIns;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetRegistrationCheckInCommandHandlerImpl implements SetRegistrationCheckInCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventRepository eventRepository;
  private final InheritedCheckIns inheritedCheckIns;

  @Override
  @Transactional
  public CheckInResult handle(SetRegistrationCheckInCommand command) {
    var registration =
        eventRegistrationRepository
            .findById(command.registrationId())
            .orElseThrow(() -> new NoSuchElementException("Registration not found"));
    if (!registration.getEvent().getId().equals(command.eventId())) {
      return inherited(registration, command);
    }
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

  /** The sign-up is the mega event's: the mark is recorded in the sub-event that inherits it. */
  private CheckInResult inherited(
      EventRegistration registration, SetRegistrationCheckInCommand command) {
    var subEvent =
        eventRepository
            .findById(command.eventId())
            .filter(found -> inheritedCheckIns.covers(found, registration))
            .orElseThrow(() -> new NoSuchElementException("Registration not found"));
    var already = false;
    java.time.LocalDateTime at = null;
    if (command.checkedIn()) {
      var mark = inheritedCheckIns.mark(registration, subEvent, CheckInSource.MANUAL);
      already = mark.already();
      at = mark.checkedInAt();
    } else {
      inheritedCheckIns.undo(registration, subEvent);
    }
    return new CheckInResult(
        registration.getId(),
        registration.getUser() == null ? null : registration.getUser().getId(),
        registration.getFullName(),
        registration.getTicketType().getName(),
        already,
        at,
        true);
  }
}
