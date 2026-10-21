package org.ouracademy.srtomy.accountability.command.create_accountability;

public interface CreateAccountabilityCommandHandler {
  CreateAccountabilityResult handle(CreateAccountabilityCommand command);
}
