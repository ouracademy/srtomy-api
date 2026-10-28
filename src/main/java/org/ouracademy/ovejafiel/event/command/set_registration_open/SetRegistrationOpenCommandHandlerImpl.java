package org.ouracademy.ovejafiel.event.command.set_registration_open;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetRegistrationOpenCommandHandlerImpl implements SetRegistrationOpenCommandHandler {

  private final EventRegistrationSetupRepository setupRepository;

  @Override
  @Transactional
  public SetRegistrationOpenResult handle(SetRegistrationOpenCommand command) {
    var setup =
        setupRepository
            .lockByEventId(command.eventId())
            .orElseThrow(() -> new NoSuchElementException("The event takes no sign-ups"));
    setup.setOpen(command.open());
    return new SetRegistrationOpenResult();
  }
}
