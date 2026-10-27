package org.ouracademy.ovejafiel.event.query.get_shared_event;

import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.query.RecurrenceResult;

/**
 * What the page of a shared event shows. {@code registration} is null when the event takes no
 * sign-ups; {@code canRegister} says this visitor can sign up now and {@code registrationStatus} is
 * set when they already have a place. {@code recurrence} is set for a repeating event.
 */
public record GetSharedEventResult(
    String title,
    String description,
    String address,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isACourse,
    String organizationName,
    RecurrenceResult recurrence,
    Registration registration,
    boolean canRegister,
    RegistrationStatus registrationStatus) {

  /** {@code soldOut} when no ticket on sale has seats left (a waitlist may still take people). */
  public record Registration(
      boolean open,
      boolean requiresAccount,
      boolean waitlistEnabled,
      boolean soldOut,
      List<Ticket> tickets) {}

  /** {@code remaining} is null when nothing limits the ticket. */
  public record Ticket(
      TicketTypeId id,
      String name,
      int priceCents,
      String currency,
      boolean onSale,
      Integer remaining) {}
}
