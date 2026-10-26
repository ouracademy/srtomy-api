package org.ouracademy.ovejafiel.invitation.command.create_invitation;

import org.ouracademy.ovejafiel.invitation.query.InvitationResult;

/** {@code emailSent} is false when the provider failed; the invitation exists and can be resent. */
public record CreateInvitationResult(InvitationResult invitation, boolean emailSent) {}
