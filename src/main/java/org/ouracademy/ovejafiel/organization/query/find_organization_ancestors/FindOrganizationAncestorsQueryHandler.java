package org.ouracademy.ovejafiel.organization.query.find_organization_ancestors;

/** The nodes above an organization, the church first and its parent last (empty for a church). */
public interface FindOrganizationAncestorsQueryHandler {
  FindOrganizationAncestorsResult handle(FindOrganizationAncestorsQuery query);
}
