package org.ouracademy.ovejafiel.iam.command.delete_role;

public interface DeleteRoleCommandHandler {
  DeleteRoleResult handle(DeleteRoleCommand command);
}
