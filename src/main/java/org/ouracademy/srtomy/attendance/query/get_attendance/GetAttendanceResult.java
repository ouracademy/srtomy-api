package org.ouracademy.srtomy.attendance.query.get_attendance;

import java.util.List;
import org.ouracademy.srtomy.attendance.domain.EventAttendance;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.party.domain.PartyId;

public record GetAttendanceResult(EventId eventId, List<PartyId> personIds) {

  public static GetAttendanceResult from(EventAttendance eventAttendance) {
    return new GetAttendanceResult(eventAttendance.getId(), eventAttendance.getPersonIds());
  }
}
