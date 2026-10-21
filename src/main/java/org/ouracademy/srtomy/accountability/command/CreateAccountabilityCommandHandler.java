package org.ouracademy.srtomy.accountability.command;

public interface CreateAccountabilityCommandHandler {
  CreateAccountabilityResult handle(CreateAccountabilityCommand command);
}
