package org.ouracademy.srtomy.organization.command;

public interface DeleteOrganizationCommandHandler {
  DeleteOrganizationResult handle(DeleteOrganizationCommand command);
}
