package org.ouracademy.srtomy.event;

import java.util.Optional;
import org.ouracademy.srtomy.event.domain.EventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, EventId> {

  Optional<EventAttendance> findByEventId(EventId id);
}
