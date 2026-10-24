package org.ouracademy.srtomy.iam.command.grant_role;

public interface GrantRoleCommandHandler {
  GrantRoleResult handle(GrantRoleCommand command);
}
