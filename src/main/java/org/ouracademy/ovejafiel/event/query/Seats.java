package org.ouracademy.ovejafiel.event.query;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.TicketType;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Component;

/**
 * How many seats are taken and how many are left. Only confirmed sign-ups hold a seat. Call it
 * under the lock of the setup ({@code lockByEventId}) when the answer decides whether to take one.
 */
@Component
@RequiredArgsConstructor
public class Seats {

  private final EventRegistrationRepository eventRegistrationRepository;

  /** Confirmed sign-ups per ticket type; a type nobody holds is absent. */
  public Map<TicketTypeId, Long> confirmedByTicket(EventId eventId) {
    var sold = new HashMap<TicketTypeId, Long>();
    for (var row :
        eventRegistrationRepository.countByTicketType(eventId, RegistrationStatus.CONFIRMED)) {
      sold.put((TicketTypeId) row[0], (Long) row[1]);
    }
    return sold;
  }

  /** Seats left for {@code ticket}, or null when nothing limits it. */
  public Integer remaining(
      EventRegistrationSetup setup, TicketType ticket, Map<TicketTypeId, Long> sold) {
    Integer left = null;
    if (ticket.getQuantity() != null) {
      left = (int) Math.max(0, ticket.getQuantity() - sold.getOrDefault(ticket.getId(), 0L));
    }
    return left;
  }

  public boolean hasRoom(
      EventRegistrationSetup setup, TicketType ticket, Map<TicketTypeId, Long> sold) {
    var left = remaining(setup, ticket, sold);
    return left == null || left > 0;
  }
}
