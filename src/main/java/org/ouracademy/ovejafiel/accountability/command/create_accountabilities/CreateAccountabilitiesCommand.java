package org.ouracademy.ovejafiel.accountability.command.create_accountabilities;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.command.create_accountability.CreateAccountabilityCommand;

/** Batch variant of {@link CreateAccountabilityCommand}, persisted with a single saveAll. */
public record CreateAccountabilitiesCommand(List<CreateAccountabilityCommand> items) {}
