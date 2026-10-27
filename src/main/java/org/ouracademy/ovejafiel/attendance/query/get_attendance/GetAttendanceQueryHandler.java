package org.ouracademy.ovejafiel.attendance.query.get_attendance;

import java.util.Optional;

public interface GetAttendanceQueryHandler {
  /** Empty when no attendance has been registered for the event yet. */
  Optional<GetAttendanceResult> handle(GetAttendanceQuery query);
}
