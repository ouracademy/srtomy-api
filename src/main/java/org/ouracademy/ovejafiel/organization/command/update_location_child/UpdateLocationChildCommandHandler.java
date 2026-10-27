package org.ouracademy.ovejafiel.organization.command.update_location_child;

import java.util.Optional;

public interface UpdateLocationChildCommandHandler {
  Optional<UpdateLocationChildResult> handle(UpdateLocationChildCommand command);
}
