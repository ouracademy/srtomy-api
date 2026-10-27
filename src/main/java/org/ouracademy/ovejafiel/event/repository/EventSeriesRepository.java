package org.ouracademy.ovejafiel.event.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.event.domain.EventSeriesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EventSeriesRepository extends JpaRepository<EventSeries, EventSeriesId> {

  /**
   * Series that still have occurrences to create before {@code threshold}, locked so that two
   * instances running the job do not create them twice.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<EventSeries> findByFullyMaterializedFalseAndMaterializedUntilBefore(LocalDate threshold);
}
