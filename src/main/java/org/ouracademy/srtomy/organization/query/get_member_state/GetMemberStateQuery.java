package org.ouracademy.srtomy.organization.query.get_member_state;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Who is a visitor / membership-in-progress / member of {@code organizationId}. */
public record GetMemberStateQuery(PartyId organizationId) {}
