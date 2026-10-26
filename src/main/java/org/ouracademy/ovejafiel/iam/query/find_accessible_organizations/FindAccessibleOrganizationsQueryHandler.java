package org.ouracademy.ovejafiel.iam.query.find_accessible_organizations;

import java.util.List;

public interface FindAccessibleOrganizationsQueryHandler {
  List<AccessibleOrganizationResult> handle(FindAccessibleOrganizationsQuery query);
}
