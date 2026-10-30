package org.ouracademy.ovejafiel.attendance.query.find_event_ids_with_attendance;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.domain.EventAttendance;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindEventIdsWithAttendanceQueryHandlerImpl implements FindEventIdsWithAttendanceQueryHandler {

  private final EventAttendanceRepository eventAttendanceRepository;

  @Override
  @Transactional(readOnly = true)
  public FindEventIdsWithAttendanceResult handle(FindEventIdsWithAttendanceQuery query) {
    if (query.eventIds().isEmpty()) {
      return new FindEventIdsWithAttendanceResult(Set.of());
    }
    return new FindEventIdsWithAttendanceResult(
        eventAttendanceRepository.findWithAttendance(query.eventIds()).stream()
            .map(EventAttendance::getId)
            .collect(Collectors.toSet()));
  }
}
