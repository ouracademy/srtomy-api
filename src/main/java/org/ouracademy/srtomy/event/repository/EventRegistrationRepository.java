package org.ouracademy.srtomy.event.repository;

import java.util.List;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.EventRegistration;
import org.ouracademy.srtomy.event.domain.EventRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository
    extends JpaRepository<EventRegistration, EventRegistrationId> {

  List<EventRegistration> findByEventIdOrderByCreateDate(EventId eventId);

  boolean existsByEventIdAndEmailIgnoreCase(EventId eventId, String email);

  long countByEventId(EventId eventId);
}
