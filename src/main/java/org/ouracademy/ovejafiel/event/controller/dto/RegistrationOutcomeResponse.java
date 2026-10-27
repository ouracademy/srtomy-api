package org.ouracademy.ovejafiel.event.controller.dto;

import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventResult;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;

/**
 * How a sign-up went. Keep {@code cancelCode} to cancel without an account and show {@code
 * ticketCode} as the QR of the ticket.
 */
public record RegistrationOutcomeResponse(
    boolean alreadyRegistered, RegistrationStatus status, String cancelCode, String ticketCode) {

  public static RegistrationOutcomeResponse from(RegisterForEventResult result) {
    return new RegistrationOutcomeResponse(
        result.alreadyRegistered(), result.status(), result.cancelCode(), result.ticketCode());
  }
}
