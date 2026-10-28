package org.ouracademy.ovejafiel.attendance.controller.dto;

import java.time.ZonedDateTime;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome.GetAttendanceWelcomeResult;

/** What someone who scanned the QR sees; {@code status} says whether they can mark themselves. */
public record AttendanceWelcomeResponse(
    String title,
    ZonedDateTime start,
    String organizationName,
    GetAttendanceWelcomeResult.Status status) {

  public static AttendanceWelcomeResponse from(GetAttendanceWelcomeResult result) {
    return new AttendanceWelcomeResponse(
        result.title(), result.start(), result.organizationName(), result.status());
  }
}
