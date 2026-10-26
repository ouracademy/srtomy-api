package org.ouracademy.ovejafiel.event.command.unshare_event;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.repository.EventShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UnshareEventCommandHandlerImpl implements UnshareEventCommandHandler {

  private final EventShareRepository eventShareRepository;

  @Override
  @Transactional
  public UnshareEventResult handle(UnshareEventCommand command) {
    var share =
        eventShareRepository
            .findById(command.shareId())
            .filter(found -> found.getEvent().getId().equals(command.eventId()))
            .orElseThrow(() -> new NoSuchElementException("Share not found"));
    eventShareRepository.delete(share);
    return new UnshareEventResult();
  }
}
