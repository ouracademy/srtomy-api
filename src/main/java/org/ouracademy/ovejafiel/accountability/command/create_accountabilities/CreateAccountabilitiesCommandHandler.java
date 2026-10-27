package org.ouracademy.ovejafiel.accountability.command.create_accountabilities;

public interface CreateAccountabilitiesCommandHandler {
  CreateAccountabilitiesResult handle(CreateAccountabilitiesCommand command);
}
