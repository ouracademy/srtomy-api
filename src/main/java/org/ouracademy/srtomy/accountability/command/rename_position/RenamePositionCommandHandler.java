package org.ouracademy.srtomy.accountability.command.rename_position;

import java.util.Optional;

/** Empty when the position does not exist; fails when the new name is taken in the church. */
public interface RenamePositionCommandHandler {
  Optional<RenamePositionResult> handle(RenamePositionCommand command);
}
