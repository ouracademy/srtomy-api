package org.ouracademy.srtomy.invitation.controller.dto;

/**
 * {@code emailSent} is true once the email left. When false the invitation exists anyway and the
 * email is retried on its own; {@code invitation.emailStatus} says whether it is still waiting or
 * gave up.
 */
public record CreateInvitationResponse(InvitationResponse invitation, boolean emailSent) {}
