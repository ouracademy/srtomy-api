package org.ouracademy.srtomy.accountability.command.create_accountabilities;

public interface CreateAccountabilitiesCommandHandler {
  CreateAccountabilitiesResult handle(CreateAccountabilitiesCommand command);
}
