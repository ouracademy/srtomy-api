package org.ouracademy.srtomy.person.command;

import java.util.Optional;

public interface UpdatePersonCommandHandler {
  /** Empty when no person has the given id. */
  Optional<UpdatePersonResult> handle(UpdatePersonCommand command);
}
