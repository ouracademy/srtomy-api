package org.ouracademy.ovejafiel.attendance.command.register_attendance;

import java.util.List;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Replaces the whole attendee list of an event with {@code personIds}. */
public record RegisterAttendanceCommand(EventId eventId, List<PartyId> personIds) {}
