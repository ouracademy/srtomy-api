package org.ouracademy.srtomy.attendance.command.delete_attendance_for_events;

public interface DeleteAttendanceForEventsCommandHandler {
  DeleteAttendanceForEventsResult handle(DeleteAttendanceForEventsCommand command);
}
