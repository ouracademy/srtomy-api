package org.ouracademy.srtomy.attendance.command.delete_attendance_for_events;

import java.util.List;
import org.ouracademy.srtomy.event.domain.EventId;

/** Soft deletes the attendance of each event. Events without attendance are skipped. */
public record DeleteAttendanceForEventsCommand(List<EventId> eventIds) {}
