package org.ouracademy.ovejafiel.attendance.command.mark_user_present;

import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * Marks the person that {@code userId} is in the church of the event, if the account is linked to
 * one. {@code markedBy} is the user who marked them, null when they marked themselves.
 */
public record MarkUserPresentCommand(
    EventId eventId, UserId userId, CheckInSource source, UserId markedBy) {}
