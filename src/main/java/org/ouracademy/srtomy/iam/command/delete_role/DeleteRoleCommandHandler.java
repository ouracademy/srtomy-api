package org.ouracademy.srtomy.iam.command.delete_role;

public interface DeleteRoleCommandHandler {
  DeleteRoleResult handle(DeleteRoleCommand command);
}
