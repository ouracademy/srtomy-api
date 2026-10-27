package org.ouracademy.ovejafiel.attendance.command.register_attendance;

import java.util.List;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record RegisterAttendanceResult(EventId eventId, List<PartyId> personIds) {}
