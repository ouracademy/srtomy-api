package org.ouracademy.ovejafiel.event.command.remove_event_registration;

/**
 * Takes the registration away from an event. Fails with {@code DomainValidationException} (400)
 * while anyone still holds a seat or waits for one: close the registration instead.
 */
public interface RemoveEventRegistrationCommandHandler {
  RemoveEventRegistrationResult handle(RemoveEventRegistrationCommand command);
}
