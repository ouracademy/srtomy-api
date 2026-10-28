package org.ouracademy.ovejafiel.event.command;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.Seats;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Component;

/**
 * Gives the seats that free up to whoever has waited longest and fits. Call it in the transaction
 * that freed them (or raised a limit), after taking the lock of the setup.
 */
@Component
@RequiredArgsConstructor
public class WaitlistPromotion {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final Seats seats;

  public int promote(EventRegistrationSetup setup) {
    var eventId = setup.getEvent().getId();
    var sold = seats.confirmedByTicket(eventId);
    var promoted = 0;
    for (var waiting :
        eventRegistrationRepository.findByEventIdAndStatusOrderByCreateDate(
            eventId, RegistrationStatus.WAITLISTED)) {
      var ticket = waiting.getTicketType();
      if (seats.hasRoom(setup, ticket, sold)) {
        waiting.confirm();
        sold.merge(ticket.getId(), 1L, Long::sum);
        promoted++;
      }
    }
    return promoted;
  }
}
