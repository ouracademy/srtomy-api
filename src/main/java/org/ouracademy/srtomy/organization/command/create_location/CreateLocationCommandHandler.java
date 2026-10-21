package org.ouracademy.srtomy.organization.command.create_location;

public interface CreateLocationCommandHandler {
  CreateLocationResult handle(CreateLocationCommand command);
}
