package org.ouracademy.srtomy.event.command.delete_event;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommand;
import org.ouracademy.srtomy.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommandHandler;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventChangeOption;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.event.repository.SingleEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteEventCommandHandlerImpl implements DeleteEventCommandHandler {

  private final SingleEventRepository singleEventRepository;
  private final DeleteAttendanceForEventsCommandHandler deleteAttendanceForEventsCommandHandler;

  @Override
  @Transactional
  public DeleteEventResult handle(DeleteEventCommand command) {
    var event = singleEventRepository.findById(command.id()).orElseThrow();
    var deleted = eventsToDelete(event, command.option());
    deleteAttendanceForEventsCommandHandler.handle(
        new DeleteAttendanceForEventsCommand(deleted.stream().map(Event::getId).toList()));
    deleted.forEach(Event::delete);
    return new DeleteEventResult(command.id());
  }

  private List<Event> eventsToDelete(SingleEvent event, EventChangeOption option) {
    var events = new ArrayList<Event>();
    switch (option) {
      case THIS_EVENT:
        events.add(event);
        break;
      case THIS_AND_THE_FOLLOWING_EVENTS:
        events.addAll(
            singleEventRepository.findByRecurrentEventAndStartAfter(
                event.getRecurrentEvent(), event.getStart()));
        events.add(event);
        break;
      case ALL_EVENTS:
        events.addAll(singleEventRepository.findByRecurrentEvent(event.getRecurrentEvent()));
        events.add(event.getRecurrentEvent());
        break;
    }
    return events;
  }
}
