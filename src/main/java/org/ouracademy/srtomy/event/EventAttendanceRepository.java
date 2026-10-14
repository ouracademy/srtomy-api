package org.ouracademy.srtomy.event;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, Long> {

  Optional<EventAttendance> findByEventId(Long id);
}
