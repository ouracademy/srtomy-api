package org.ouracademy.ovejafiel.event.command.cancel_event_registration;

/**
 * Cancels a sign-up on behalf of the organizers and gives its seat to whoever waits. Fails with
 * {@code NoSuchElementException} (404) when it does not exist or belongs to another event.
 */
public interface CancelEventRegistrationCommandHandler {
  CancelEventRegistrationResult handle(CancelEventRegistrationCommand command);
}
