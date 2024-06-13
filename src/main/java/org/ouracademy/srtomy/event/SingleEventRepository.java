package org.ouracademy.srtomy.event;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SingleEventRepository extends JpaRepository<SingleEvent, Long> {

    List<SingleEvent> findByRecurrentEvent(RecurrentEvent recurrentEvent);

    List<SingleEvent> findByRecurrentEventAndStartAfter(RecurrentEvent recurrentEvent, ZonedDateTime start);
}
