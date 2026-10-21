package org.ouracademy.srtomy.organization.command.create_church;

public interface CreateChurchCommandHandler {
  CreateChurchResult handle(CreateChurchCommand command);
}
