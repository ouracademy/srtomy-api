package org.ouracademy.ovejafiel.event.command.cancel_event_registration;

/**
 * Removes a sign-up. Fails with {@code NoSuchElementException} (404) when it does not exist or
 * belongs to another event.
 */
public interface CancelEventRegistrationCommandHandler {
  CancelEventRegistrationResult handle(CancelEventRegistrationCommand command);
}
