package org.ouracademy.srtomy.event;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {

    // @Query("select e from Event e where e.start ")
    // List<Event> searchEventsBetween(LocalDateTime start, LocalDateTime end);

}
