package org.ouracademy.ovejafiel.attendance.query.find_event_ids_with_attendance;

public interface FindEventIdsWithAttendanceQueryHandler {
  /** Which of the events have someone marked present or visitors counted. */
  FindEventIdsWithAttendanceResult handle(FindEventIdsWithAttendanceQuery query);
}
