package org.ouracademy.ovejafiel.accountability.command.create_position;

/** Fails with a {@code DomainValidationException} when the church already has that name. */
public interface CreatePositionCommandHandler {
  CreatePositionResult handle(CreatePositionCommand command);
}
