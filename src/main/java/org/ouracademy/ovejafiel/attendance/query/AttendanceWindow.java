package org.ouracademy.ovejafiel.attendance.query;

import java.time.Duration;
import java.time.ZonedDateTime;

/** When people can mark their own attendance by scanning the QR: around the event, not always. */
public final class AttendanceWindow {

  public static final Duration BEFORE = Duration.ofHours(1);
  public static final Duration AFTER = Duration.ofHours(3);

  private AttendanceWindow() {}

  public static boolean isOpen(ZonedDateTime start, ZonedDateTime end, ZonedDateTime now) {
    return !now.isBefore(start.minus(BEFORE)) && !now.isAfter(end.plus(AFTER));
  }
}
