package org.ouracademy.srtomy.organization.command;

import java.util.Optional;

public interface EditOrganizationCommandHandler {
  /** Empty when no organization has the given id. */
  Optional<EditOrganizationResult> handle(EditOrganizationCommand command);
}
