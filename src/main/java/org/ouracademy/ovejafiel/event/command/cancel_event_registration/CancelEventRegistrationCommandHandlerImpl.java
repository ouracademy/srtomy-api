package org.ouracademy.ovejafiel.event.command.cancel_event_registration;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.WaitlistPromotion;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CancelEventRegistrationCommandHandlerImpl implements CancelEventRegistrationCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventRegistrationSetupRepository setupRepository;
  private final WaitlistPromotion waitlistPromotion;

  @Override
  @Transactional
  public CancelEventRegistrationResult handle(CancelEventRegistrationCommand command) {
    // Lock first, so the seat that frees up is counted by whoever comes next.
    var setup = setupRepository.lockByEventId(command.eventId());
    var registration =
        eventRegistrationRepository
            .findById(command.registrationId())
            .filter(found -> found.getEvent().getId().equals(command.eventId()))
            .orElseThrow(() -> new NoSuchElementException("Registration not found"));
    if (registration.getStatus().isActive()) {
      registration.cancel();
      eventRegistrationRepository.flush();
      setup.ifPresent(waitlistPromotion::promote);
    }
    return new CancelEventRegistrationResult();
  }
}
