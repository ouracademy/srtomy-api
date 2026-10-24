package org.ouracademy.srtomy.invitation.command.accept_invitation;

import org.ouracademy.srtomy.invitation.domain.InvitationId;

/** {@code subject} is the signed-in user who accepts; their verified email must be the invited. */
public record AcceptInvitationCommand(String subject, InvitationId invitationId) {}
