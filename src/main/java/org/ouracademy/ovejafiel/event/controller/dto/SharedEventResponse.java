package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventResult;

/**
 * The page of a shared event: what it is, where and when, how to sign up, and where the visitor
 * stands. {@code registration} is null when the event takes no sign-ups.
 */
public record SharedEventResponse(
    String title,
    String description,
    String address,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isACourse,
    String organizationName,
    RecurrenceResponse recurrence,
    RegistrationResponse registration,
    boolean canRegister,
    RegistrationStatus registrationStatus,
    List<ProgramItemResponse> program,
    ContainerResponse container) {

  /** A sub-event of a mega event; never lists who signed up. */
  public record ProgramItemResponse(
      String slug,
      String title,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isACourse,
      RegistrationMode registrationMode,
      boolean includedInTicket) {}

  /** The mega event a sub-event belongs to. */
  public record ContainerResponse(String slug, String title) {}

  public record RegistrationResponse(
      boolean open, boolean requiresAccount, boolean soldOut, List<TicketResponse> tickets) {}

  /** {@code remaining} is null when nothing limits the ticket. */
  public record TicketResponse(
      UUID id, String name, int priceCents, String currency, boolean onSale, Integer remaining) {}

  public static SharedEventResponse from(GetSharedEventResult result) {
    var registration = result.registration();
    return new SharedEventResponse(
        result.title(),
        result.description(),
        result.address(),
        result.start(),
        result.end(),
        result.isACourse(),
        result.organizationName(),
        RecurrenceResponse.from(result.recurrence()),
        registration == null
            ? null
            : new RegistrationResponse(
                registration.open(),
                registration.requiresAccount(),
                registration.soldOut(),
                registration.tickets().stream()
                    .map(
                        ticket ->
                            new TicketResponse(
                                ticket.id().value(),
                                ticket.name(),
                                ticket.priceCents(),
                                ticket.currency(),
                                ticket.onSale(),
                                ticket.remaining()))
                    .toList()),
        result.canRegister(),
        result.registrationStatus(),
        result.program().stream()
            .map(
                item ->
                    new ProgramItemResponse(
                        item.slug(),
                        item.title(),
                        item.start(),
                        item.end(),
                        item.isACourse(),
                        item.registrationMode(),
                        item.includedInTicket()))
            .toList(),
        result.container() == null
            ? null
            : new ContainerResponse(result.container().slug(), result.container().title()));
  }
}
