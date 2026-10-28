package org.ouracademy.ovejafiel.event.command.check_in_user_registration;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.CheckInResult;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CheckInUserRegistrationCommandHandlerImpl implements CheckInUserRegistrationCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;

  @Override
  @Transactional
  public Optional<CheckInResult> handle(CheckInUserRegistrationCommand command) {
    return find(command)
        .filter(found -> found.getStatus() == RegistrationStatus.CONFIRMED)
        .map(
            registration -> {
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

  private Optional<EventRegistration> find(CheckInUserRegistrationCommand command) {
    var byUser =
        eventRegistrationRepository.findFirstByEventIdAndUserIdAndStatusNot(
            command.eventId(), command.userId(), RegistrationStatus.CANCELLED);
    if (byUser.isPresent() || command.email() == null) {
      return byUser;
    }
    return eventRegistrationRepository.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(
        command.eventId(), command.email(), RegistrationStatus.CANCELLED);
  }
}
