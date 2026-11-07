package org.ouracademy.ovejafiel.event.repository;

import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventNoticeRepository extends JpaRepository<EventNotice, UUID> {

  /** The newest first. */
  List<EventNotice> findTop20ByEventIdOrderByCreatedAtDesc(EventId eventId);
}
