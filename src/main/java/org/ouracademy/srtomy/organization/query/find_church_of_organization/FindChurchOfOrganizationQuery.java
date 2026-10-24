package org.ouracademy.srtomy.organization.query.find_church_of_organization;

import org.ouracademy.srtomy.party.domain.PartyId;

/** The church (tree root) that {@code organizationId} belongs to; a church is its own church. */
public record FindChurchOfOrganizationQuery(PartyId organizationId) {}
