package org.ouracademy.ovejafiel.organization.query.find_church_of_organization;

import java.util.Optional;

public interface FindChurchOfOrganizationQueryHandler {
  Optional<FindChurchOfOrganizationResult> handle(FindChurchOfOrganizationQuery query);
}
