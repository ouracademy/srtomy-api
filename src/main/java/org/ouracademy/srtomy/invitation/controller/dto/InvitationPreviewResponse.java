package org.ouracademy.srtomy.invitation.controller.dto;

import org.ouracademy.srtomy.invitation.query.InvitationResult;

/** What the invitee sees before signing in: where they are invited, as what, and to which email. */
public record InvitationPreviewResponse(
    String email, String organizationName, String positionName, String status) {

  public static InvitationPreviewResponse from(InvitationResult result) {
    return new InvitationPreviewResponse(
        result.email(), result.organizationName(), result.positionName(), result.status().name());
  }
}
