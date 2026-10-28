package org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome;

import java.util.Optional;

/** Empty when the code was not made by us (altered) or its event no longer exists. */
public interface GetAttendanceWelcomeQueryHandler {
  Optional<GetAttendanceWelcomeResult> handle(GetAttendanceWelcomeQuery query);
}
