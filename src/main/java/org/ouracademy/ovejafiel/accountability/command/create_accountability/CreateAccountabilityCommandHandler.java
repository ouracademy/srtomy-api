package org.ouracademy.ovejafiel.accountability.command.create_accountability;

public interface CreateAccountabilityCommandHandler {
  CreateAccountabilityResult handle(CreateAccountabilityCommand command);
}
