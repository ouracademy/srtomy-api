package org.ouracademy.ovejafiel.attendance.command.set_attendance_guests;

/**
 * Fails with {@code NoSuchElementException} (404) when the event does not exist and with {@code
 * DomainValidationException} (400) when the count is negative.
 */
public interface SetAttendanceGuestsCommandHandler {
  SetAttendanceGuestsResult handle(SetAttendanceGuestsCommand command);
}
