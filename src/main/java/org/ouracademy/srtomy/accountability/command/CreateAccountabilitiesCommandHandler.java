package org.ouracademy.srtomy.accountability.command;

public interface CreateAccountabilitiesCommandHandler {
  CreateAccountabilitiesResult handle(CreateAccountabilitiesCommand command);
}
