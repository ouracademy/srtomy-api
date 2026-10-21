package org.ouracademy.srtomy.organization.query;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Who is a visitor / membership-in-progress / member of {@code organizationId}. */
public record GetMemberStateQuery(PartyId organizationId) {}
