package org.ouracademy.ovejafiel.event.command.cancel_registration_by_code;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.WaitlistPromotion;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CancelRegistrationByCodeCommandHandlerImpl implements CancelRegistrationByCodeCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventRegistrationSetupRepository setupRepository;
  private final WaitlistPromotion waitlistPromotion;

  @Override
  @Transactional
  public CancelRegistrationByCodeResult handle(CancelRegistrationByCodeCommand command) {
    var registration =
        eventRegistrationRepository
            .findByCancelCode(command.cancelCode() == null ? "" : command.cancelCode())
            .orElseThrow(() -> new NoSuchElementException("Registration not found"));
    var setup = setupRepository.lockByEventId(registration.getEvent().getId());
    if (registration.getStatus().isActive()) {
      registration.cancel();
      eventRegistrationRepository.flush();
      setup.ifPresent(waitlistPromotion::promote);
    }
    return new CancelRegistrationByCodeResult();
  }
}
