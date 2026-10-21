package org.ouracademy.srtomy.organization.command;

import java.util.Optional;

public interface UpdateLocationCommandHandler {
  /** Empty when no organization has the given id. */
  Optional<UpdateLocationResult> handle(UpdateLocationCommand command);
}
