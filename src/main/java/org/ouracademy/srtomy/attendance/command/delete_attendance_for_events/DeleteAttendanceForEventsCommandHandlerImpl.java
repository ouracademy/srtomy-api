package org.ouracademy.srtomy.attendance.command.delete_attendance_for_events;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.attendance.domain.EventAttendance;
import org.ouracademy.srtomy.attendance.repository.EventAttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteAttendanceForEventsCommandHandlerImpl
    implements DeleteAttendanceForEventsCommandHandler {

  private final EventAttendanceRepository eventAttendanceRepository;

  @Override
  @Transactional
  public DeleteAttendanceForEventsResult handle(DeleteAttendanceForEventsCommand command) {
    // Dirty checking flushes the change as an UPDATE at commit.
    eventAttendanceRepository.findAllById(command.eventIds()).forEach(EventAttendance::delete);
    return new DeleteAttendanceForEventsResult();
  }
}
