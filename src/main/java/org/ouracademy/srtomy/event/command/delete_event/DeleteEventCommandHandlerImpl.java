package org.ouracademy.srtomy.event.command.delete_event;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.event.repository.SingleEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteEventCommandHandlerImpl implements DeleteEventCommandHandler {

  private final EventRepository eventRepository;
  private final SingleEventRepository singleEventRepository;

  @Override
  @Transactional
  public DeleteEventResult handle(DeleteEventCommand command) {
    var event = singleEventRepository.findById(command.id()).orElseThrow();
    switch (command.option()) {
      case THIS_EVENT:
        eventRepository.delete(event);
        break;
      case THIS_AND_THE_FOLLOWING_EVENTS:
        var followingEvents =
            singleEventRepository.findByRecurrentEventAndStartAfter(
                event.getRecurrentEvent(), event.getStart());
        eventRepository.deleteAll(followingEvents);
        eventRepository.delete(event);
        break;
      case ALL_EVENTS:
        eventRepository.deleteAll(
            singleEventRepository.findByRecurrentEvent(event.getRecurrentEvent()));
        eventRepository.delete(event.getRecurrentEvent());
        break;
    }
    return new DeleteEventResult(command.id());
  }
}
