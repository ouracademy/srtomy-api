package org.ouracademy.ovejafiel.organization.query.find_organization_children;

import java.util.List;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;

public interface FindOrganizationChildrenQueryHandler {
  List<FindOrganizationResult> handle(FindOrganizationChildrenQuery query);
}
