package org.ouracademy.srtomy.event.repository;

import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.RecurrentEvent;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SingleEventRepository extends JpaRepository<SingleEvent, EventId> {

  List<SingleEvent> findByRecurrentEvent(RecurrentEvent recurrentEvent);

  List<SingleEvent> findByRecurrentEventAndStartAfter(
      RecurrentEvent recurrentEvent, ZonedDateTime start);
}
