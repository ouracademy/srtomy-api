package org.ouracademy.ovejafiel.attendance.query.get_attendance_qr;

/** Fails with {@code NoSuchElementException} (404) when the event does not exist. */
public interface GetAttendanceQrQueryHandler {
  GetAttendanceQrResult handle(GetAttendanceQrQuery query);
}
