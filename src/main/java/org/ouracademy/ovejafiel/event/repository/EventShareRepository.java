package org.ouracademy.ovejafiel.event.repository;

import java.util.List;
import java.util.Optional;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventShare;
import org.ouracademy.ovejafiel.event.domain.EventShareId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventShareRepository extends JpaRepository<EventShare, EventShareId> {

  List<EventShare> findByEventIdOrderByCreateDate(EventId eventId);

  Optional<EventShare> findByEventIdAndEmailIgnoreCase(EventId eventId, String email);

  List<EventShare> findByEmailIgnoreCase(String email);
}
