package org.ouracademy.ovejafiel.organization.command.delete_organization;

public interface DeleteOrganizationCommandHandler {
  DeleteOrganizationResult handle(DeleteOrganizationCommand command);
}
