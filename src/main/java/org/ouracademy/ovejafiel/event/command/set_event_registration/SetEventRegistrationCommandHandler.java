package org.ouracademy.ovejafiel.event.command.set_event_registration;

/**
 * Fails with {@code NoSuchElementException} (404) when the event or a ticket does not exist, and
 * with {@code DomainValidationException} (400) when the setup makes no sense or removes a ticket
 * that already has sign-ups.
 */
public interface SetEventRegistrationCommandHandler {
  SetEventRegistrationResult handle(SetEventRegistrationCommand command);
}
