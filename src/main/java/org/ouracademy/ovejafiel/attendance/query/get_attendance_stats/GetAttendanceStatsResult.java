package org.ouracademy.ovejafiel.attendance.query.get_attendance_stats;

import java.time.LocalDate;
import java.util.List;

/** Attendance stats of a range, sliced into periods, oldest first. */
public record GetAttendanceStatsResult(List<Bucket> buckets) {

  /**
   * One period: how many {@code eventsCount} happened, how many {@code attendanceMarks} were
   * recorded (people marked present plus unnamed guests), how many {@code uniqueAttendees} that was
   * (distinct people; guests are not people, so they are not counted here), and the {@code
   * averagePerEvent} of marks.
   */
  public record Bucket(
      LocalDate periodStart,
      LocalDate periodEnd,
      String label,
      long eventsCount,
      long attendanceMarks,
      long uniqueAttendees,
      double averagePerEvent) {}
}
