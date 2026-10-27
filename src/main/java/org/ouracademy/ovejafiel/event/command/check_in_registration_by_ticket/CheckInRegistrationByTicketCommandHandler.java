package org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket;

import org.ouracademy.ovejafiel.event.command.CheckInResult;

/**
 * Fails with {@code NoSuchElementException} (404) when the code is not a ticket of this event, and
 * with {@code DomainValidationException} (400) when the sign-up is waiting or cancelled.
 */
public interface CheckInRegistrationByTicketCommandHandler {
  CheckInResult handle(CheckInRegistrationByTicketCommand command);
}
