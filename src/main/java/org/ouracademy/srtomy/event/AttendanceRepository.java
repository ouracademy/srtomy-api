package org.ouracademy.srtomy.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    void deleteAllByEvent(Event event);
}
