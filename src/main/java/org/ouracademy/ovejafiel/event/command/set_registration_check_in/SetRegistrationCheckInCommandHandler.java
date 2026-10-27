package org.ouracademy.ovejafiel.event.command.set_registration_check_in;

import org.ouracademy.ovejafiel.event.command.CheckInResult;

/**
 * Fails with {@code NoSuchElementException} (404) when the sign-up is not of this event, and with
 * {@code DomainValidationException} (400) when marking one that is waiting or cancelled.
 */
public interface SetRegistrationCheckInCommandHandler {
  CheckInResult handle(SetRegistrationCheckInCommand command);
}
