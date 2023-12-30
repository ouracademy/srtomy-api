package org.ouracademy.srtomy.event;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    void deleteAllByEvent(Event event);

    @Query("select a.person.id from Attendance a where a.event.id = :eventId")
    List<Long> findPersonIdsByEventId(Long eventId);
}
