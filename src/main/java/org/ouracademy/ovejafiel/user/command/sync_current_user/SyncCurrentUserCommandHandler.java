package org.ouracademy.ovejafiel.user.command.sync_current_user;

public interface SyncCurrentUserCommandHandler {
  SyncCurrentUserResult handle(SyncCurrentUserCommand command);
}
