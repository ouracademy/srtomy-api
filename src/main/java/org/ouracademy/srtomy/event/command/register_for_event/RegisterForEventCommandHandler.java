package org.ouracademy.srtomy.event.command.register_for_event;

/**
 * Signs someone up through the link of a course. Fails with {@code NoSuchElementException} (404)
 * when the link does not exist, with {@code AccessDeniedException} (403) when the link does not
 * allow signing up this visitor, and with {@code DomainValidationException} (400) for a missing
 * name or an invalid email.
 */
public interface RegisterForEventCommandHandler {
  RegisterForEventResult handle(RegisterForEventCommand command);
}
