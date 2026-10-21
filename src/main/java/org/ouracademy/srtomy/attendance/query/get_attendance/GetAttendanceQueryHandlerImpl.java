package org.ouracademy.srtomy.attendance.query.get_attendance;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.attendance.repository.EventAttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetAttendanceQueryHandlerImpl implements GetAttendanceQueryHandler {

  private final EventAttendanceRepository eventAttendanceRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<GetAttendanceResult> handle(GetAttendanceQuery query) {
    return eventAttendanceRepository.findByEventId(query.eventId()).map(GetAttendanceResult::from);
  }
}
