package org.ouracademy.ovejafiel.iam.command.create_role;

public interface CreateRoleCommandHandler {
  CreateRoleResult handle(CreateRoleCommand command);
}
