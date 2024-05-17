package org.ouracademy.srtomy.event;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {

    // t: -----|----|----
    // e: --|-|--------- X

    // t: -----|----|----
    // e: ----|--|-------

    // t: -----|----|----
    // e: -------|-|-----

    // t: -----|----|----
    // e: -------|----|--

    // t: -----|----|---- X
    // e: -----------|-|-
    @Query("select e from Event e where e.start <= :end and e.end >= :start")
    List<Event> searchEventsBetween(ZonedDateTime start, ZonedDateTime end);

}
