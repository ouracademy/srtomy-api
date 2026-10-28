package org.ouracademy.ovejafiel.attendance.query.get_attendance_qr;

import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.query.AttendanceQrToken;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetAttendanceQrQueryHandlerImpl implements GetAttendanceQrQueryHandler {

  private final AttendanceQrToken attendanceQrToken;
  private final FindEventByIdQueryHandler findEventByIdQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public GetAttendanceQrResult handle(GetAttendanceQrQuery query) {
    findEventByIdQueryHandler
        .handle(new FindEventByIdQuery(query.eventId()))
        .orElseThrow(
            () -> new NoSuchElementException("Event not found: " + query.eventId().value()));
    var now = Instant.now();
    return new GetAttendanceQrResult(
        attendanceQrToken.issue(query.eventId(), now), attendanceQrToken.refreshAt(now));
  }
}
