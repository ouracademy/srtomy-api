package org.ouracademy.ovejafiel.attendance.query.get_attendance_summary;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetAttendanceSummaryQueryHandlerImpl implements GetAttendanceSummaryQueryHandler {

  private final EventAttendanceRepository eventAttendanceRepository;

  @Override
  @Transactional(readOnly = true)
  public GetAttendanceSummaryResult handle(GetAttendanceSummaryQuery query) {
    var totals = eventAttendanceRepository.totalsOf(query.eventId().value());
    return new GetAttendanceSummaryResult(
        totals.getPeople(), totals.getTicketOnly(), totals.getGuests());
  }
}
