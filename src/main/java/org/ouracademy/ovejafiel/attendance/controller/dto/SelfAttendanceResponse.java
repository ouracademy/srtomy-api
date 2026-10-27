package org.ouracademy.ovejafiel.attendance.controller.dto;

import org.ouracademy.ovejafiel.attendance.command.mark_self_attendance.MarkSelfAttendanceResult;

/** What the scan did, and whether they had already been marked. */
public record SelfAttendanceResponse(MarkSelfAttendanceResult.Kind kind, boolean alreadyMarked) {

  public static SelfAttendanceResponse from(MarkSelfAttendanceResult result) {
    return new SelfAttendanceResponse(result.kind(), result.alreadyMarked());
  }
}
