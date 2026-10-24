package org.ouracademy.srtomy.accountability.command.delete_position;

public interface DeletePositionCommandHandler {
  DeletePositionResult handle(DeletePositionCommand command);
}
