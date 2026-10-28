package org.ouracademy.ovejafiel.event.command.set_event_registration;

import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;

/**
 * Turns on, or changes, the registration of an event. {@code tickets} is the whole list: one with
 * an {@code id} updates that ticket, one without creates it, and one left out is removed.
 */
public record SetEventRegistrationCommand(
    EventId eventId,
    boolean open,
    boolean requiresAccount,
    boolean waitlistEnabled,
    Integer capacity,
    List<Ticket> tickets) {

  public record Ticket(
      TicketTypeId id,
      String name,
      int priceCents,
      String currency,
      Integer quantity,
      ZonedDateTime salesStart,
      ZonedDateTime salesEnd) {}
}
