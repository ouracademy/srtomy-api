package org.ouracademy.ovejafiel.organization.command.update_location;

import java.util.Optional;

public interface UpdateLocationCommandHandler {
  /** Empty when no organization has the given id. */
  Optional<UpdateLocationResult> handle(UpdateLocationCommand command);
}
