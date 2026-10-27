package org.ouracademy.ovejafiel.event.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EventRegistrationSetupRepository
    extends JpaRepository<EventRegistrationSetup, EventRegistrationSetupId> {

  Optional<EventRegistrationSetup> findByEventId(EventId eventId);

  /** Serializes the commands that take or give back seats of one event. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from EventRegistrationSetup s where s.event.id = :eventId")
  Optional<EventRegistrationSetup> lockByEventId(EventId eventId);
}
