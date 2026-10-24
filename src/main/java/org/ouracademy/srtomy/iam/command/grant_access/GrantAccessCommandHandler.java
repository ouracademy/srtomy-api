package org.ouracademy.srtomy.iam.command.grant_access;

public interface GrantAccessCommandHandler {
  GrantAccessResult handle(GrantAccessCommand command);
}
