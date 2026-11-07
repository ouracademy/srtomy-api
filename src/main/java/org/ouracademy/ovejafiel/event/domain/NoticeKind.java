package org.ouracademy.ovejafiel.event.domain;

/** What a notice of an event is about. */
public enum NoticeKind {
  /** The organizers wrote it. */
  ANNOUNCEMENT,
  /** The date, hour, name, place or speakers changed. */
  CHANGED,
  /** A sub-event of the program was cancelled. */
  CANCELLED,
  /** How the event is entered changed and some sign-ups no longer count. */
  REGISTRATION_CHANGED
}
