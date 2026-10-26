package org.ouracademy.ovejafiel.invitation.command.revoke_invitation;

import org.ouracademy.ovejafiel.invitation.domain.InvitationId;

public record RevokeInvitationCommand(InvitationId invitationId) {}
