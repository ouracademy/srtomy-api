package org.ouracademy.ovejafiel.event.command.check_in_user_registration;

import java.util.Optional;
import org.ouracademy.ovejafiel.event.command.CheckInResult;

/** Empty when that user has no confirmed seat in the event. */
public interface CheckInUserRegistrationCommandHandler {
  Optional<CheckInResult> handle(CheckInUserRegistrationCommand command);
}
