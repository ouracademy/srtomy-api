package org.ouracademy.ovejafiel.attendance.query.get_attendance_qr;

import java.time.Instant;

/** The code to show in the QR; ask for a new one at {@code refreshAt}. */
public record GetAttendanceQrResult(String token, Instant refreshAt) {}
