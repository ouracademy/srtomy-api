package org.ouracademy.ovejafiel.attendance.query.get_attendance;

import java.util.List;
import org.ouracademy.ovejafiel.attendance.domain.EventAttendance;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record GetAttendanceResult(EventId eventId, List<PartyId> personIds) {

  public static GetAttendanceResult from(EventAttendance eventAttendance) {
    return new GetAttendanceResult(eventAttendance.getId(), eventAttendance.getPersonIds());
  }
}
