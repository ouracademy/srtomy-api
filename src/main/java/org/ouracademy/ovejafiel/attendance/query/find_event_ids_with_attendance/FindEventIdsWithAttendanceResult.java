package org.ouracademy.ovejafiel.attendance.query.find_event_ids_with_attendance;

import java.util.Set;
import org.ouracademy.ovejafiel.event.domain.EventId;

public record FindEventIdsWithAttendanceResult(Set<EventId> eventIds) {}
