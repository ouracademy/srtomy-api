package org.ouracademy.ovejafiel.event.command.delete_event;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommand;
import org.ouracademy.ovejafiel.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommandHandler;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteEventCommandHandlerImpl implements DeleteEventCommandHandler {

  private final EventRepository eventRepository;
  private final DeleteAttendanceForEventsCommandHandler deleteAttendanceForEventsCommandHandler;

  @Override
  @Transactional
  public DeleteEventResult handle(DeleteEventCommand command) {
    var event = eventRepository.findById(command.id()).orElseThrow();
    var deleted = eventsToDelete(event, command.option());
    deleteAttendanceForEventsCommandHandler.handle(
        new DeleteAttendanceForEventsCommand(deleted.stream().map(Event::getId).toList()));
    deleted.forEach(Event::delete);
    stopSeries(event, command.option());
    return new DeleteEventResult(command.id());
  }

  /** A one-off event only has itself, whatever the option. */
  private List<Event> eventsToDelete(Event event, EventChangeOption option) {
    var series = event.getSeries();
    if (series == null) {
      return List.of(event);
    }
    return switch (option) {
      case THIS_EVENT -> List.of(event);
      case THIS_AND_THE_FOLLOWING_EVENTS ->
          eventRepository.findBySeriesAndStartGreaterThanEqual(series, event.getStart());
      case ALL_EVENTS -> eventRepository.findBySeries(series);
    };
  }

  /** The rule stops producing occurrences from the deleted day on, or at all. */
  private void stopSeries(Event event, EventChangeOption option) {
    var series = event.getSeries();
    if (series == null) {
      return;
    }
    switch (option) {
      case THIS_EVENT -> {}
      case THIS_AND_THE_FOLLOWING_EVENTS ->
          series.endOn(
              event.getStart().withZoneSameInstant(series.zone()).toLocalDate().minusDays(1));
      case ALL_EVENTS -> series.delete();
    }
  }
}
