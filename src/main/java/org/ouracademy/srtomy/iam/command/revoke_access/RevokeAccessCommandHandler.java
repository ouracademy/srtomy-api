package org.ouracademy.srtomy.iam.command.revoke_access;

public interface RevokeAccessCommandHandler {
  RevokeAccessResult handle(RevokeAccessCommand command);
}
