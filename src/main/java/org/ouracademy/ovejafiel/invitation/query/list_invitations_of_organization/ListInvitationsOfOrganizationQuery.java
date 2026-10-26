package org.ouracademy.ovejafiel.invitation.query.list_invitations_of_organization;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The invitations of {@code organizationId} that were neither accepted nor revoked. */
public record ListInvitationsOfOrganizationQuery(PartyId organizationId) {}
