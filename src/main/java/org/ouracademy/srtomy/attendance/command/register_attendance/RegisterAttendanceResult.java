package org.ouracademy.srtomy.attendance.command.register_attendance;

import java.util.List;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.party.domain.PartyId;

public record RegisterAttendanceResult(EventId eventId, List<PartyId> personIds) {}
