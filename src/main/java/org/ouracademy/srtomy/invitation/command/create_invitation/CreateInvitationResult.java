package org.ouracademy.srtomy.invitation.command.create_invitation;

import org.ouracademy.srtomy.invitation.query.InvitationResult;

/** {@code emailSent} is false when the provider failed; the invitation exists and can be resent. */
public record CreateInvitationResult(InvitationResult invitation, boolean emailSent) {}
