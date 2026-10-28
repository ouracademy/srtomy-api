package org.ouracademy.ovejafiel.event.repository;

import java.util.List;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.TicketType;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTypeRepository extends JpaRepository<TicketType, TicketTypeId> {

  List<TicketType> findByEventIdOrderByPosition(EventId eventId);
}
