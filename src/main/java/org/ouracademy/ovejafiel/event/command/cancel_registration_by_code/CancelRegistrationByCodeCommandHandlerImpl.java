package org.ouracademy.ovejafiel.event.command.cancel_registration_by_code;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CancelRegistrationByCodeCommandHandlerImpl implements CancelRegistrationByCodeCommandHandler {

  private final EventRegistrationRepository eventRegistrationRepository;

  @Override
  @Transactional
  public CancelRegistrationByCodeResult handle(CancelRegistrationByCodeCommand command) {
    var registration =
        eventRegistrationRepository
            .findByCancelCode(command.cancelCode() == null ? "" : command.cancelCode())
            .orElseThrow(() -> new NoSuchElementException("Registration not found"));
    if (registration.getStatus().isActive()) {
      registration.cancel();
    }
    return new CancelRegistrationByCodeResult();
  }
}
