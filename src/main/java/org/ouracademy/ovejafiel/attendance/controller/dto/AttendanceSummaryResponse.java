package org.ouracademy.ovejafiel.attendance.controller.dto;

import org.ouracademy.ovejafiel.attendance.query.get_attendance_summary.GetAttendanceSummaryResult;

/**
 * How many came to the event, counting each person once: {@code people} marked in the attendance,
 * {@code ticketOnly} who came in with a ticket without being one of those people, {@code guests}
 * visitors without a name, and the {@code total} of the three.
 */
public record AttendanceSummaryResponse(long people, long ticketOnly, long guests, long total) {

  public static AttendanceSummaryResponse from(GetAttendanceSummaryResult result) {
    return new AttendanceSummaryResponse(
        result.people(), result.ticketOnly(), result.guests(), result.total());
  }
}
