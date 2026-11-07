package org.ouracademy.ovejafiel.attendance.query.get_attendance_summary;

/**
 * How many came, counting each person once: {@code people} are marked in the attendance, {@code
 * ticketOnly} came in with a ticket and are not one of those people, {@code guests} are visitors
 * without a name. {@code total} is the sum.
 */
public record GetAttendanceSummaryResult(long people, long ticketOnly, long guests) {

  public long total() {
    return people + ticketOnly + guests;
  }
}
