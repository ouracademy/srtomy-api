package org.ouracademy.srtomy.attendance.query;

import org.ouracademy.srtomy.event.domain.EventId;

public record GetAttendanceQuery(EventId eventId) {}
