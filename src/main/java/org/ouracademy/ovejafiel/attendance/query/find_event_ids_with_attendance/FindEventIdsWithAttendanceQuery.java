package org.ouracademy.ovejafiel.attendance.query.find_event_ids_with_attendance;

import java.util.Collection;
import org.ouracademy.ovejafiel.event.domain.EventId;

public record FindEventIdsWithAttendanceQuery(Collection<EventId> eventIds) {}
