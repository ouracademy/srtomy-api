package org.ouracademy.ovejafiel.event.command.set_registration_open;

/**
 * Closes the registration of an event, so nobody else can sign up, or opens it again. The setup and
 * the sign-ups are kept. Fails with {@code NoSuchElementException} (404) when the event takes no
 * sign-ups.
 */
public interface SetRegistrationOpenCommandHandler {
  SetRegistrationOpenResult handle(SetRegistrationOpenCommand command);
}
