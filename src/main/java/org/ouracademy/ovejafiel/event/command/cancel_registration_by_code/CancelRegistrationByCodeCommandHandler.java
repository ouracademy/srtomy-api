package org.ouracademy.ovejafiel.event.command.cancel_registration_by_code;

/**
 * Fails with {@code NoSuchElementException} (404) when no sign-up has that code. Cancelling twice
 * is not an error.
 */
public interface CancelRegistrationByCodeCommandHandler {
  CancelRegistrationByCodeResult handle(CancelRegistrationByCodeCommand command);
}
