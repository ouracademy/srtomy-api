package org.ouracademy.ovejafiel.event.command;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * Who was checked in. {@code userId} is the account that signed up, if any, so the attendance
 * module can mark the person linked to it. {@code alreadyCheckedIn} when they had been before.
 * {@code inherited} when the ticket is the container's and the check-in belongs to a sub-event: the
 * sign-up itself is left untouched and the attendance module marks the person in that sub-event.
 */
public record CheckInResult(
    EventRegistrationId registrationId,
    UserId userId,
    String name,
    String ticketName,
    boolean alreadyCheckedIn,
    LocalDateTime checkedInAt,
    boolean inherited) {

  public CheckInResult(
      EventRegistrationId registrationId,
      UserId userId,
      String name,
      String ticketName,
      boolean alreadyCheckedIn,
      LocalDateTime checkedInAt) {
    this(registrationId, userId, name, ticketName, alreadyCheckedIn, checkedInAt, false);
  }
}
