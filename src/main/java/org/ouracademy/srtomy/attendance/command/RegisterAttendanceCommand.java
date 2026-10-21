package org.ouracademy.srtomy.attendance.command;

import java.util.List;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Replaces the whole attendee list of an event with {@code personIds}. */
public record RegisterAttendanceCommand(EventId eventId, List<PartyId> personIds) {}
