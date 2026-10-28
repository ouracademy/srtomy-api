package org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.query.AttendanceQrToken;
import org.ouracademy.ovejafiel.attendance.query.AttendanceWindow;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome.GetAttendanceWelcomeResult.Status;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetAttendanceWelcomeQueryHandlerImpl implements GetAttendanceWelcomeQueryHandler {

  private final AttendanceQrToken attendanceQrToken;
  private final FindEventByIdQueryHandler findEventByIdQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public Optional<GetAttendanceWelcomeResult> handle(GetAttendanceWelcomeQuery query) {
    return attendanceQrToken
        .verify(query.token(), Instant.now())
        .flatMap(
            verified ->
                findEventByIdQueryHandler
                    .handle(new FindEventByIdQuery(verified.eventId()))
                    .map(
                        event ->
                            new GetAttendanceWelcomeResult(
                                event.title(),
                                event.start(),
                                event.organization().name(),
                                statusOf(
                                    verified.fresh(),
                                    AttendanceWindow.isOpen(
                                        event.start(), event.end(), ZonedDateTime.now())))));
  }

  private static Status statusOf(boolean fresh, boolean inWindow) {
    if (!fresh) {
      return Status.EXPIRED_CODE;
    }
    return inWindow ? Status.OPEN : Status.OUT_OF_WINDOW;
  }
}
