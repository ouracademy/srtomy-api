package org.ouracademy.ovejafiel.event.repository;

import java.util.List;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository
    extends JpaRepository<EventRegistration, EventRegistrationId> {

  List<EventRegistration> findByEventIdOrderByCreateDate(EventId eventId);

  boolean existsByEventIdAndEmailIgnoreCase(EventId eventId, String email);

  long countByEventId(EventId eventId);
}
