package org.ouracademy.ovejafiel.invitation.controller.dto;

import org.ouracademy.ovejafiel.invitation.query.InvitationResult;

/**
 * What the invitee sees before signing in: where they are invited, as what, and to which email.
 * {@code needsName} says the invitee will be asked for their name when accepting.
 */
public record InvitationPreviewResponse(
    String email, String organizationName, String positionName, String status, boolean needsName) {

  public static InvitationPreviewResponse from(InvitationResult result) {
    return new InvitationPreviewResponse(
        result.email(),
        result.organizationName(),
        result.positionName(),
        result.status().name(),
        result.needsName());
  }
}
