package org.ouracademy.ovejafiel.invitation.controller.dto;

/** {@code emailSent} is false when the provider failed: the invitation exists and can be resent. */
public record CreateInvitationResponse(InvitationResponse invitation, boolean emailSent) {}
