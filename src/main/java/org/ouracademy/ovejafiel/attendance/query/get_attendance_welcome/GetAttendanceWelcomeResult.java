package org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome;

import java.time.ZonedDateTime;

/**
 * The event of a scanned code and whether it can take their attendance: {@code OPEN}, {@code
 * EXPIRED_CODE} (ask for the current QR) or {@code OUT_OF_WINDOW} (the event is not on now).
 */
public record GetAttendanceWelcomeResult(
    String title, ZonedDateTime start, String organizationName, Status status) {

  public enum Status {
    OPEN,
    EXPIRED_CODE,
    OUT_OF_WINDOW
  }
}
