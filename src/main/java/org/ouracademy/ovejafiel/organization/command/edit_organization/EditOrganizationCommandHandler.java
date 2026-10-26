package org.ouracademy.ovejafiel.organization.command.edit_organization;

import java.util.Optional;

public interface EditOrganizationCommandHandler {
  /** Empty when no organization has the given id. */
  Optional<EditOrganizationResult> handle(EditOrganizationCommand command);
}
