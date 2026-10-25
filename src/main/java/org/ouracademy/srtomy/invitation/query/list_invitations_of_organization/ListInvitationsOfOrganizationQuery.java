package org.ouracademy.srtomy.invitation.query.list_invitations_of_organization;

import org.ouracademy.srtomy.party.domain.PartyId;

/** The invitations of {@code organizationId} that were neither accepted nor revoked. */
public record ListInvitationsOfOrganizationQuery(PartyId organizationId) {}
