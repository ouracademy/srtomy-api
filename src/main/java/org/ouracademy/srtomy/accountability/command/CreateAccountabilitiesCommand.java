package org.ouracademy.srtomy.accountability.command;

import java.util.List;

/** Batch variant of {@link CreateAccountabilityCommand}, persisted with a single saveAll. */
public record CreateAccountabilitiesCommand(List<CreateAccountabilityCommand> items) {}
