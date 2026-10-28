package org.ouracademy.ovejafiel.event.command;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * Who was checked in. {@code userId} is the account that signed up, if any, so the attendance
 * module can mark the person linked to it. {@code alreadyCheckedIn} when they had been before.
 */
public record CheckInResult(
    EventRegistrationId registrationId,
    UserId userId,
    String name,
    String ticketName,
    boolean alreadyCheckedIn,
    LocalDateTime checkedInAt) {}
