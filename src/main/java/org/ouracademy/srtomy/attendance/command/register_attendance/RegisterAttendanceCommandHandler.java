package org.ouracademy.srtomy.attendance.command.register_attendance;

public interface RegisterAttendanceCommandHandler {
  /**
   * Creates the event's attendance if it does not exist yet, then sets its attendees.
   *
   * @throws java.util.NoSuchElementException if the event or any of the people does not exist
   */
  RegisterAttendanceResult handle(RegisterAttendanceCommand command);
}
