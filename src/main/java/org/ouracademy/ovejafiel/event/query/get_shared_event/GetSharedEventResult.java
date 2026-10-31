package org.ouracademy.ovejafiel.event.query.get_shared_event;

import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.query.RecurrenceResult;

/**
 * What the page of a shared event shows. {@code registration} is null when the event takes no
 * sign-ups; {@code canRegister} says this visitor can sign up now and {@code registrationStatus} is
 * set when they already have a place. {@code recurrence} is set for a repeating event. A mega event
 * lists its {@code program}; a sub-event shows the registration in force (its own or its
 * container's, where {@code canRegister} is then false: they sign up in the {@code container}).
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
    RegistrationStatus registrationStatus,
    List<ProgramItem> program,
    Container container) {

  /**
   * A sub-event of a mega event the visitor can open. {@code includedInTicket} when the ticket of
   * the mega event is enough to enter it.
   */
  public record ProgramItem(
      String slug,
      String title,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isACourse,
      RegistrationMode registrationMode,
      boolean includedInTicket) {}

  /** The mega event a sub-event belongs to, when the visitor can open it too. */
  public record Container(String slug, String title) {}

  /** {@code soldOut} when no ticket on sale has seats left. */
  public record Registration(
      boolean open, boolean requiresAccount, boolean soldOut, List<Ticket> tickets) {}

  /** {@code remaining} is null when nothing limits the ticket. */
  public record Ticket(
      TicketTypeId id,
      String name,
      int priceCents,
      String currency,
      boolean onSale,
      Integer remaining) {}
}
