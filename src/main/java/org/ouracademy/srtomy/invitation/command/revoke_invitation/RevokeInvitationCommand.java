package org.ouracademy.srtomy.invitation.command.revoke_invitation;

import org.ouracademy.srtomy.invitation.domain.InvitationId;

public record RevokeInvitationCommand(InvitationId invitationId) {}
