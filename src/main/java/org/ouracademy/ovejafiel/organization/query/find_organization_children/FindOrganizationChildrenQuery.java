package org.ouracademy.ovejafiel.organization.query.find_organization_children;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The organizations (not people) that are IS_PART_OF {@code parentId}. */
public record FindOrganizationChildrenQuery(PartyId parentId) {}
