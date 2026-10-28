package org.ouracademy.ovejafiel.attendance.command.set_attendance_guests;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetAttendanceGuestsCommandHandlerImpl implements SetAttendanceGuestsCommandHandler {

  private final EventAttendanceRepository eventAttendanceRepository;
  private final FindEventByIdQueryHandler findEventByIdQueryHandler;

  @Override
  @Transactional
  public SetAttendanceGuestsResult handle(SetAttendanceGuestsCommand command) {
    findEventByIdQueryHandler
        .handle(new FindEventByIdQuery(command.eventId()))
        .orElseThrow(
            () -> new NoSuchElementException("Event not found: " + command.eventId().value()));
    eventAttendanceRepository.createIfMissing(command.eventId().value());
    var attendance =
        eventAttendanceRepository
            .findByEventId(command.eventId())
            .orElseThrow(
                () -> new NoSuchElementException("Event not found: " + command.eventId().value()));
    attendance.setGuestCount(command.guestCount());
    eventAttendanceRepository.save(attendance);
    return new SetAttendanceGuestsResult(attendance.getGuestCount());
  }
}
