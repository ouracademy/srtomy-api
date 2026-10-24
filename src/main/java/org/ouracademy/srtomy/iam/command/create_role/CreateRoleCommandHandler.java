package org.ouracademy.srtomy.iam.command.create_role;

public interface CreateRoleCommandHandler {
  CreateRoleResult handle(CreateRoleCommand command);
}
