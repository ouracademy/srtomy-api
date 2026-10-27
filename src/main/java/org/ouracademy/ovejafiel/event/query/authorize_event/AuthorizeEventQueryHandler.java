package org.ouracademy.ovejafiel.event.query.authorize_event;

/**
 * Fails with {@code NoSuchElementException} (404) when the event does not exist and with {@code
 * AccessDeniedException} (403) when the user cannot do at least what was asked with it.
 */
public interface AuthorizeEventQueryHandler {
  AuthorizeEventResult handle(AuthorizeEventQuery query);
}
