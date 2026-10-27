package org.ouracademy.ovejafiel.organization.command.create_location;

public interface CreateLocationCommandHandler {
  CreateLocationResult handle(CreateLocationCommand command);
}
