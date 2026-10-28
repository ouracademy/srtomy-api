package org.ouracademy.ovejafiel.attendance.command.mark_user_present;

import java.util.Optional;

/** Empty when the user is not linked to a person of the church the event belongs to. */
public interface MarkUserPresentCommandHandler {
  Optional<MarkUserPresentResult> handle(MarkUserPresentCommand command);
}
