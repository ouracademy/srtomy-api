package org.ouracademy.ovejafiel.event.command.set_registration_open;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.WaitlistPromotion;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetRegistrationOpenCommandHandlerImpl implements SetRegistrationOpenCommandHandler {

  private final EventRegistrationSetupRepository setupRepository;
  private final WaitlistPromotion waitlistPromotion;

  @Override
  @Transactional
  public SetRegistrationOpenResult handle(SetRegistrationOpenCommand command) {
    var setup =
        setupRepository
            .lockByEventId(command.eventId())
            .orElseThrow(() -> new NoSuchElementException("The event takes no sign-ups"));
    setup.setOpen(command.open());
    // Whoever waits for a seat may get it now that the registration is open again.
    if (setup.isOpen()) {
      waitlistPromotion.promote(setup);
    }
    return new SetRegistrationOpenResult();
  }
}
