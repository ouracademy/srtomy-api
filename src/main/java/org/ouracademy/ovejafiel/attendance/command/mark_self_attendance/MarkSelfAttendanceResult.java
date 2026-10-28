package org.ouracademy.ovejafiel.attendance.command.mark_self_attendance;

/**
 * What the scan did. {@code PERSON}: the person of the account was marked. {@code REGISTRATION}:
 * their ticket was checked in. {@code VISITOR}: a visitor was registered with the name they gave.
 * {@code ANONYMOUS}: one more visitor without a name was counted. {@code alreadyMarked} when they
 * had been marked before.
 */
public record MarkSelfAttendanceResult(Kind kind, boolean alreadyMarked) {

  public enum Kind {
    PERSON,
    REGISTRATION,
    VISITOR,
    ANONYMOUS
  }
}
