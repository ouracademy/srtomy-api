package org.ouracademy.srtomy.organization.command;

public interface CreateLocationCommandHandler {
  CreateLocationResult handle(CreateLocationCommand command);
}
