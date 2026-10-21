package org.ouracademy.srtomy.organization.query.find_organization_children;

import java.util.List;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;

public interface FindOrganizationChildrenQueryHandler {
  List<FindOrganizationResult> handle(FindOrganizationChildrenQuery query);
}
