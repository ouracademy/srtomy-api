package org.ouracademy.ovejafiel.attendance.command.mark_person_present;

import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * Adds {@code personId} to the attendees of the event without touching the others. {@code markedBy}
 * is the user who marked them, null when they marked themselves.
 */
public record MarkPersonPresentCommand(
    EventId eventId, PartyId personId, CheckInSource source, UserId markedBy) {}
