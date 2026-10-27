package org.ouracademy.ovejafiel.organization.command.create_church;

public interface CreateChurchCommandHandler {
  CreateChurchResult handle(CreateChurchCommand command);
}
