package org.ouracademy.srtomy.user.command.sync_current_user;

public interface SyncCurrentUserCommandHandler {
  SyncCurrentUserResult handle(SyncCurrentUserCommand command);
}
