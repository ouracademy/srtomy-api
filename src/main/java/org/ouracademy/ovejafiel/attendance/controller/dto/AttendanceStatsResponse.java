package org.ouracademy.ovejafiel.attendance.controller.dto;

import java.time.LocalDate;
import java.util.List;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_stats.GetAttendanceStatsResult;

/** Attendance over a date range, sliced into periods. */
public record AttendanceStatsResponse(List<AttendanceBucketResponse> buckets) {

  public record AttendanceBucketResponse(
      LocalDate periodStart,
      LocalDate periodEnd,
      String label,
      long eventsCount,
      long attendanceMarks,
      long uniqueAttendees,
      double averagePerEvent) {}

  public static AttendanceStatsResponse from(GetAttendanceStatsResult result) {
    return new AttendanceStatsResponse(
        result.buckets().stream()
            .map(
                bucket ->
                    new AttendanceBucketResponse(
                        bucket.periodStart(),
                        bucket.periodEnd(),
                        bucket.label(),
                        bucket.eventsCount(),
                        bucket.attendanceMarks(),
                        bucket.uniqueAttendees(),
                        bucket.averagePerEvent()))
            .toList());
  }
}
