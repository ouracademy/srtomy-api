package org.ouracademy.srtomy.organization.query.find_organization_subtree;

import org.ouracademy.srtomy.party.domain.PartyId;

/** The node {@code rootId} and all its descendants in the tree. */
public record FindOrganizationSubtreeQuery(PartyId rootId) {}
