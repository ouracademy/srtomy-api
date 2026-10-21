package org.ouracademy.srtomy.organization.command.delete_organization;

public interface DeleteOrganizationCommandHandler {
  DeleteOrganizationResult handle(DeleteOrganizationCommand command);
}
