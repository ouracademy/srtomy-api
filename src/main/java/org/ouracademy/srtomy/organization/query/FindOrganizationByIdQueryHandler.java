package org.ouracademy.srtomy.organization.query;

import java.util.Optional;

public interface FindOrganizationByIdQueryHandler {
  /** Empty when no organization has the given id (including when it is a person's id). */
  Optional<FindOrganizationResult> handle(FindOrganizationByIdQuery query);
}
