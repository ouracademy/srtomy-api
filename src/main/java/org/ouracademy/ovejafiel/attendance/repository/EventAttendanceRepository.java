package org.ouracademy.ovejafiel.attendance.repository;

import java.util.Optional;
import org.ouracademy.ovejafiel.attendance.domain.EventAttendance;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, EventId> {

  Optional<EventAttendance> findByEventId(EventId id);
}
