package org.ouracademy.ovejafiel.event.query.get_event_registration;

import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;

/**
 * {@code enabled} is false (and the rest empty) when the event has no registration. {@code sold}
 * counts the confirmed sign-ups of each ticket.
 */
public record GetEventRegistrationResult(
    boolean enabled,
    boolean open,
    boolean requiresAccount,
    long confirmed,
    long waitlisted,
    long checkedIn,
    List<Ticket> tickets) {

  public record Ticket(
      TicketTypeId id,
      String name,
      int priceCents,
      String currency,
      Integer quantity,
      ZonedDateTime salesStart,
      ZonedDateTime salesEnd,
      long sold) {}

  public static GetEventRegistrationResult disabled() {
    return new GetEventRegistrationResult(false, false, false, 0, 0, 0, List.of());
  }
}
