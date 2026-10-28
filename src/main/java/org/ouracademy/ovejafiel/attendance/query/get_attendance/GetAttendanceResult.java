package org.ouracademy.ovejafiel.attendance.query.get_attendance;

import java.time.LocalDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.attendance.domain.EventAttendance;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Who was present: the people marked (each with how and when) and how many visitors without a name
 * came.
 */
public record GetAttendanceResult(
    EventId eventId, List<PartyId> personIds, int guestCount, List<Mark> marks) {

  public record Mark(PartyId personId, CheckInSource source, LocalDateTime markedAt) {}

  public static GetAttendanceResult from(EventAttendance eventAttendance) {
    return new GetAttendanceResult(
        eventAttendance.getId(),
        eventAttendance.getPersonIds(),
        eventAttendance.getGuestCount(),
        eventAttendance.getPersonEventAttendances().stream()
            .map(mark -> new Mark(mark.getPerson().getId(), mark.getSource(), mark.getDateTime()))
            .toList());
  }
}
