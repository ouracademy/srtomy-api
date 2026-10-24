package org.ouracademy.srtomy.accountability.command.set_position_access;

import java.util.Optional;

/** Empty when the position does not exist. */
public interface SetPositionAccessCommandHandler {
  Optional<SetPositionAccessResult> handle(SetPositionAccessCommand command);
}
