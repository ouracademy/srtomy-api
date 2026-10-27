package org.ouracademy.ovejafiel.invitation.command.accept_invitation;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Where the user landed: the node they were invited to, its church and their person record. */
public record AcceptInvitationResult(PartyId organizationId, PartyId churchId, PartyId personId) {}
