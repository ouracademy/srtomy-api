package org.ouracademy.ovejafiel.attendance.command.set_attendance_guests;

import org.ouracademy.ovejafiel.event.domain.EventId;

/** Sets how many visitors without a name came to the event. */
public record SetAttendanceGuestsCommand(EventId eventId, int guestCount) {}
