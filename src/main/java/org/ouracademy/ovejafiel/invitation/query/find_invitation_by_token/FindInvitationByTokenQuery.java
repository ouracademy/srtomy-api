package org.ouracademy.ovejafiel.invitation.query.find_invitation_by_token;

/** Looks an invitation up by the token of its email link. */
public record FindInvitationByTokenQuery(String token) {}
