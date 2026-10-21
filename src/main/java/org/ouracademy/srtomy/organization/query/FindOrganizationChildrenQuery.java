package org.ouracademy.srtomy.organization.query;

import org.ouracademy.srtomy.party.domain.PartyId;

/** The organizations (not people) that are IS_PART_OF {@code parentId}. */
public record FindOrganizationChildrenQuery(PartyId parentId) {}
