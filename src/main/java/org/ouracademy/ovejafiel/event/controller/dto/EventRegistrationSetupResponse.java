package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.event.query.get_event_registration.GetEventRegistrationResult;

/**
 * How the event takes sign-ups, for its organizers. {@code enabled} is false when it takes none.
 * {@code sold} counts the confirmed sign-ups of each ticket.
 */
public record EventRegistrationSetupResponse(
    boolean enabled,
    boolean open,
    boolean requiresAccount,
    long confirmed,
    long waitlisted,
    long checkedIn,
    List<TicketSetup> tickets) {

  public record TicketSetup(
      UUID id,
      String name,
      int priceCents,
      String currency,
      Integer quantity,
      ZonedDateTime salesStart,
      ZonedDateTime salesEnd,
      long sold) {}

  public static EventRegistrationSetupResponse from(GetEventRegistrationResult result) {
    return new EventRegistrationSetupResponse(
        result.enabled(),
        result.open(),
        result.requiresAccount(),
        result.confirmed(),
        result.waitlisted(),
        result.checkedIn(),
        result.tickets().stream()
            .map(
                ticket ->
                    new TicketSetup(
                        ticket.id().value(),
                        ticket.name(),
                        ticket.priceCents(),
                        ticket.currency(),
                        ticket.quantity(),
                        ticket.salesStart(),
                        ticket.salesEnd(),
                        ticket.sold()))
            .toList());
  }
}
