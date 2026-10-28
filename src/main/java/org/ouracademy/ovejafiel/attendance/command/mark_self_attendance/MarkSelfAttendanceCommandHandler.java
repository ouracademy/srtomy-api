package org.ouracademy.ovejafiel.attendance.command.mark_self_attendance;

/**
 * Fails with {@code NoSuchElementException} (404) when the code was not made by us, and with {@code
 * DomainValidationException} (400) when it has expired or the event is not taking attendance now.
 */
public interface MarkSelfAttendanceCommandHandler {
  MarkSelfAttendanceResult handle(MarkSelfAttendanceCommand command);
}
