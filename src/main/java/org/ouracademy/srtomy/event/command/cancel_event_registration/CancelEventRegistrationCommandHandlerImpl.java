package org.ouracademy.srtomy.event.command.cancel_event_registration;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CancelEventRegistrationCommandHandlerImpl implements CancelEventRegistrationCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;

  @Override
  @Transactional
  public CancelEventRegistrationResult handle(CancelEventRegistrationCommand command) {
    var registration =
        eventRegistrationRepository
            .findById(command.registrationId())
            .filter(found -> found.getEvent().getId().equals(command.eventId()))
            .orElseThrow(() -> new NoSuchElementException("Registration not found"));
    eventRegistrationRepository.delete(registration);
    return new CancelEventRegistrationResult();
  }
}
