package org.ouracademy.srtomy.organization.query;

import java.util.List;

public interface FindOrganizationChildrenQueryHandler {
  List<FindOrganizationResult> handle(FindOrganizationChildrenQuery query);
}
