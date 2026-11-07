package org.ouracademy.ovejafiel.event.command.set_event_sharing;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetEventSharingCommandHandlerImpl implements SetEventSharingCommandHandler {

  private final EventRepository eventRepository;

  @Override
  @Transactional
  public SetEventSharingResult handle(SetEventSharingCommand command) {
    var event =
        eventRepository
            .findById(command.eventId())
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    event.shareVia(command.generalAccess());
    if (command.applyToProgram()) {
      eventRepository
          .findByParentOrderByStart(event)
          .forEach(child -> child.shareVia(command.generalAccess()));
    }
    return new SetEventSharingResult();
  }
}
