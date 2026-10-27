package org.ouracademy.ovejafiel.organization.query.find_organization_by_id;

import java.util.Optional;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;

public interface FindOrganizationByIdQueryHandler {
  /** Empty when no organization has the given id (including when it is a person's id). */
  Optional<FindOrganizationResult> handle(FindOrganizationByIdQuery query);
}
